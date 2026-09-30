package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.dto.SlotAvailability;
import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import com.campus.seatreservation.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预约服务并发/一致性测试。
 *
 * 每个用例自建独立的自习室与测试用户，用例结束后清理，避免污染既有数据。
 * 依赖真实 MySQL + Redis（Redisson 分布式锁），运行前请确保二者就绪。
 */
@SpringBootTest
public class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private StudyRoomService studyRoomService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StudyRoomMapper studyRoomMapper;

    /** 当前用例创建的自习室ID，用于清理 */
    private Long currentRoomId;
    /** 当前用例创建的用户ID，用于清理 */
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        // 先删自习室：reservation/time_slot 均 ON DELETE CASCADE，会一并清理
        if (currentRoomId != null) {
            studyRoomMapper.deleteById(currentRoomId);
            currentRoomId = null;
        }
        for (Long uid : createdUserIds) {
            userMapper.deleteById(uid);
        }
        createdUserIds.clear();
    }

    /** 创建含单个时段(08:00-12:00)、指定容量的测试自习室，返回其首个时段ID */
    private Long createTestRoom(int capacity) {
        RoomRequest req = new RoomRequest();
        req.setName("测试自习室-" + UUID.randomUUID().toString().substring(0, 8));
        req.setTotalCapacity(capacity);
        RoomRequest.TimeSlotItem slot = new RoomRequest.TimeSlotItem();
        slot.setStartTime("08:00:00");
        slot.setEndTime("12:00:00");
        req.setTimeSlots(List.of(slot));
        RoomResponse resp = studyRoomService.createRoom(req);
        currentRoomId = resp.getId();
        return resp.getTimeSlots().get(0).getId();
    }

    /** 创建 n 个测试用户，返回其ID列表 */
    private List<Long> createTestUsers(int n) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            User u = new User();
            u.setUsername("tu_" + UUID.randomUUID().toString().substring(0, 10));
            u.setPassword("x");
            u.setRole("student");
            u.setPhone("");
            userMapper.insert(u);
            ids.add(u.getId());
            createdUserIds.add(u.getId());
        }
        return ids;
    }

    /**
     * 并发预约（容量充足）：10 个不同用户抢容量 50 的同一时段，应全部成功，剩余 40。
     */
    @Test
    public void testConcurrentReserveWithinCapacity() throws Exception {
        int threadCount = 10;
        int capacity = 50;
        Long slotId = createTestRoom(capacity);
        Long roomId = currentRoomId;
        List<Long> userIds = createTestUsers(threadCount);
        LocalDate date = LocalDate.now().plusDays(1);

        int success = runConcurrentReserve(userIds, roomId, slotId, date);

        assertEquals(threadCount, success, "容量充足时应全部预约成功");
        assertEquals(capacity - threadCount, remaining(roomId, slotId, date), "剩余座位应等于容量减成功数");
    }

    /**
     * 并发预约（超出容量）：8 个不同用户抢容量 3 的同一时段，应恰好成功 3 个，不超卖，剩余 0。
     */
    @Test
    public void testConcurrentReserveOverCapacity() throws Exception {
        int threadCount = 8;
        int capacity = 3;
        Long slotId = createTestRoom(capacity);
        Long roomId = currentRoomId;
        List<Long> userIds = createTestUsers(threadCount);
        LocalDate date = LocalDate.now().plusDays(1);

        int success = runConcurrentReserve(userIds, roomId, slotId, date);

        assertEquals(capacity, success, "成功数不得超过容量（防超卖）");
        assertEquals(0, remaining(roomId, slotId, date), "约满后剩余应为 0");
    }

    /**
     * 取消后可重约：同一用户对同一(房间+时段+日期)预约→取消→再预约，应成功（验证唯一索引修复）。
     */
    @Test
    public void testRebookAfterCancel() {
        Long slotId = createTestRoom(5);
        Long roomId = currentRoomId;
        Long userId = createTestUsers(1).get(0);
        LocalDate date = LocalDate.now().plusDays(1);

        ReserveResponse first = reservationService.reserve(userId, buildReq(roomId, slotId, date));
        assertNotNull(first.getId());

        reservationService.cancel(first.getId(), userId);

        // 取消后重新预约同一时段应成功
        ReserveResponse second = reservationService.reserve(userId, buildReq(roomId, slotId, date));
        assertNotNull(second.getId());
        assertEquals("booked", second.getStatus());
    }

    // ---------------- 辅助方法 ----------------

    private ReserveRequest buildReq(Long roomId, Long slotId, LocalDate date) {
        ReserveRequest req = new ReserveRequest();
        req.setRoomId(roomId);
        req.setTimeSlotId(slotId);
        req.setReservationDate(date);
        return req;
    }

    private int remaining(Long roomId, Long slotId, LocalDate date) {
        List<SlotAvailability> list = studyRoomService.getAvailability(roomId, date);
        return list.stream()
                .filter(s -> s.getSlotId().equals(slotId))
                .findFirst()
                .map(SlotAvailability::getRemaining)
                .orElse(-1);
    }

    /** 让给定用户并发预约同一(房间+时段+日期)，返回成功数 */
    private int runConcurrentReserve(List<Long> userIds, Long roomId, Long slotId, LocalDate date)
            throws InterruptedException {
        int threadCount = userIds.size();
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final Long userId = userIds.get(i);
            threads[i] = new Thread(() -> {
                try {
                    barrier.await();
                    reservationService.reserve(userId, buildReq(roomId, slotId, date));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // 名额已满/重复预约等预期失败
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertTrue(successCount.get() >= 0);
        return successCount.get();
    }
}
