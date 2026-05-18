package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.ReserveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Test
    public void testConcurrentReserve() throws Exception {
        int threadCount = 10; // 10 个线程同时抢
        Long roomId = 1L;     // RoomA-1，容量50
        Long timeSlotId = 1L;
        Long startUserId = 2L; // 从 userId=2 开始

        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final long userId = startUserId + i;
            threads[i] = new Thread(() -> {
                try {
                    barrier.await(); // 所有线程同时出发
                    ReserveRequest req = new ReserveRequest();
                    req.setRoomId(roomId);
                    req.setTimeSlotId(timeSlotId);
                    req.setReservationDate(LocalDate.now().plusDays(1));
                    reservationService.reserve(userId, req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("成功: " + successCount.get() + ", 失败: " + failCount.get());
        // 每次测试结果可能不同，但成功的预约数不会超过 available_capacity
    }
}