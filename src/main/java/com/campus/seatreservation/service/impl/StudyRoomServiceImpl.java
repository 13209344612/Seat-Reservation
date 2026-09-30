package com.campus.seatreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.dto.SlotAvailability;
import com.campus.seatreservation.entity.Reservation;
import com.campus.seatreservation.entity.StudyRoom;
import com.campus.seatreservation.entity.TimeSlot;
import com.campus.seatreservation.mapper.ReservationMapper;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import com.campus.seatreservation.mapper.TimeSlotMapper;
import com.campus.seatreservation.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自习室业务层实现
 *
 * 处理自习室的 CRUD 与时段管理。座位余量不再以计数器存储，而是按「房间+日期+时段」
 * 维度从 reservation 表实时派生。管理员编辑/删除做安全化处理，避免因外键级联静默删除预约。
 */
@Service
@RequiredArgsConstructor
public class StudyRoomServiceImpl implements StudyRoomService {
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final ReservationMapper reservationMapper;

    /**
     * 获取所有自习室列表
     */
    @Override
    public List<RoomResponse> listRooms() {
        List<StudyRoom> rooms = studyRoomMapper.selectList(null);
        return rooms.stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取自习室详情
     */
    @Override
    public RoomResponse getRoomById(Long id) {
        StudyRoom room = studyRoomMapper.selectById(id);
        if (room == null) {
            throw new RuntimeException("自习室不存在");
        }
        return toRoomResponse(room);
    }

    /**
     * 创建自习室（事务操作）
     */
    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        StudyRoom room = new StudyRoom();
        room.setName(request.getName());
        room.setTotalCapacity(request.getTotalCapacity());
        studyRoomMapper.insert(room);

        for (RoomRequest.TimeSlotItem item : request.getTimeSlots()) {
            TimeSlot slot = new TimeSlot();
            slot.setRoomId(room.getId());
            slot.setStartTime(LocalTime.parse(item.getStartTime()));
            slot.setEndTime(LocalTime.parse(item.getEndTime()));
            timeSlotMapper.insert(slot);
        }

        return toRoomResponse(room);
    }

    /**
     * 更新自习室（事务操作）—— 时段差异化更新，保护已有预约
     */
    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        StudyRoom room = studyRoomMapper.selectById(id);
        if (room == null) {
            throw new RuntimeException("自习室不存在");
        }

        // 容量下调保护：新容量不得小于任一(时段,未来日期)当前活跃预约数的最大值
        int newCapacity = request.getTotalCapacity();
        if (newCapacity < room.getTotalCapacity()) {
            int maxBooked = maxActiveBookingsPerSlot(id);
            if (newCapacity < maxBooked) {
                throw new RuntimeException("总容量不能低于已预约数量（当前单时段单日最高预约 " + maxBooked + " 人）");
            }
        }
        room.setName(request.getName());
        room.setTotalCapacity(newCapacity);
        studyRoomMapper.updateById(room);

        // 时段差异化：带 id 就地更新，不带 id 新增，缺失的旧时段仅当无预约引用时才删除
        List<TimeSlot> existing = getTimeSlotsByRoomId(id);
        Map<Long, TimeSlot> existingMap = existing.stream()
                .collect(Collectors.toMap(TimeSlot::getId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (RoomRequest.TimeSlotItem item : request.getTimeSlots()) {
            LocalTime start = LocalTime.parse(item.getStartTime());
            LocalTime end = LocalTime.parse(item.getEndTime());
            if (item.getId() != null && existingMap.containsKey(item.getId())) {
                TimeSlot slot = existingMap.get(item.getId());
                slot.setStartTime(start);
                slot.setEndTime(end);
                timeSlotMapper.updateById(slot);
                keepIds.add(slot.getId());
            } else {
                TimeSlot slot = new TimeSlot();
                slot.setRoomId(id);
                slot.setStartTime(start);
                slot.setEndTime(end);
                timeSlotMapper.insert(slot);
                keepIds.add(slot.getId());
            }
        }

        for (TimeSlot old : existing) {
            if (keepIds.contains(old.getId())) {
                continue;
            }
            Long refCount = reservationMapper.selectCount(
                    new LambdaQueryWrapper<Reservation>().eq(Reservation::getTimeSlotId, old.getId()));
            if (refCount > 0) {
                throw new RuntimeException(String.format("存在预约的时段不可删除：%s-%s",
                        old.getStartTime(), old.getEndTime()));
            }
            timeSlotMapper.deleteById(old.getId());
        }

        return toRoomResponse(room);
    }

    /**
     * 删除自习室（事务操作）—— 有预约记录时拦截，避免外键级联静默删除预约
     */
    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>().eq(Reservation::getRoomId, id));
        if (reservationCount > 0) {
            throw new RuntimeException("该自习室已有预约记录，无法删除");
        }

        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getRoomId, id);
        timeSlotMapper.delete(wrapper);

        studyRoomMapper.deleteById(id);
    }

    /**
     * 查询某自习室在指定日期下各时段的座位余量（派生统计）
     */
    @Override
    public List<SlotAvailability> getAvailability(Long roomId, LocalDate date) {
        StudyRoom room = studyRoomMapper.selectById(roomId);
        if (room == null) {
            throw new RuntimeException("自习室不存在");
        }
        List<TimeSlot> slots = getTimeSlotsByRoomId(roomId);
        if (slots.isEmpty()) {
            return Collections.emptyList();
        }

        // 单条 group-by 统计该日期下各时段的活跃预约数，避免 N+1
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.select("time_slot_id", "COUNT(*) AS cnt")
                .eq("room_id", roomId)
                .eq("reservation_date", date)
                .in("status", "booked", "signed")
                .groupBy("time_slot_id");
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : reservationMapper.selectMaps(qw)) {
            Object sid = row.get("time_slot_id");
            Object cnt = row.get("cnt");
            if (sid != null) {
                countMap.put(((Number) sid).longValue(), cnt == null ? 0 : ((Number) cnt).intValue());
            }
        }

        int total = room.getTotalCapacity();
        return slots.stream()
                .map(s -> new SlotAvailability(
                        s.getId(),
                        s.getStartTime(),
                        s.getEndTime(),
                        total,
                        Math.max(0, total - countMap.getOrDefault(s.getId(), 0))))
                .collect(Collectors.toList());
    }

    /**
     * 统计某自习室「单时段单日」未来活跃预约数的最大值，用于容量下调保护
     */
    private int maxActiveBookingsPerSlot(Long roomId) {
        QueryWrapper<Reservation> qw = new QueryWrapper<>();
        qw.select("time_slot_id", "reservation_date", "COUNT(*) AS cnt")
                .eq("room_id", roomId)
                .in("status", "booked", "signed")
                .ge("reservation_date", LocalDate.now())
                .groupBy("time_slot_id", "reservation_date");
        int max = 0;
        for (Map<String, Object> row : reservationMapper.selectMaps(qw)) {
            Object cnt = row.get("cnt");
            if (cnt != null) {
                max = Math.max(max, ((Number) cnt).intValue());
            }
        }
        return max;
    }

    /**
     * 将实体转换为响应DTO
     */
    private RoomResponse toRoomResponse(StudyRoom room) {
        List<TimeSlot> slots = getTimeSlotsByRoomId(room.getId());
        List<RoomResponse.TimeSlotItem> slotItems = slots.stream()
                .map(s -> new RoomResponse.TimeSlotItem(
                        s.getId(), s.getStartTime(), s.getEndTime()))
                .collect(Collectors.toList());
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getTotalCapacity(),
                room.getCreateTime(),
                slotItems
        );
    }

    /**
     * 根据自习室ID获取时段列表
     */
    private List<TimeSlot> getTimeSlotsByRoomId(Long roomId) {
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getRoomId, roomId);
        return timeSlotMapper.selectList(wrapper);
    }
}
