package com.campus.seatreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.entity.StudyRoom;
import com.campus.seatreservation.entity.TimeSlot;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import com.campus.seatreservation.mapper.TimeSlotMapper;
import com.campus.seatreservation.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自习室业务层实现
 * 处理自习室的CRUD操作和时段管理，支持事务控制。
 */
@Service
@RequiredArgsConstructor
public class StudyRoomServiceImpl implements StudyRoomService {
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;

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
        // 1. 保存自习室
        StudyRoom room = new StudyRoom();
        room.setName(request.getName());
        room.setTotalCapacity(request.getTotalCapacity());
        room.setAvailableCapacity(request.getTotalCapacity()); // 新建时可用容量 = 总容量
        studyRoomMapper.insert(room);

        // 2. 保存时段
        saveTimeSlots(room.getId(), request.getTimeSlots());

        return toRoomResponse(room);
    }

    /**
     * 更新自习室（事务操作）
     */
    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        StudyRoom room = studyRoomMapper.selectById(id);
        if (room == null) {
            throw new RuntimeException("自习室不存在");
        }

        // 1. 更新自习室信息，调整可用容量
        int capacityDiff = request.getTotalCapacity() - room.getTotalCapacity();
        room.setName(request.getName());
        room.setTotalCapacity(request.getTotalCapacity());
        room.setAvailableCapacity(room.getAvailableCapacity() + capacityDiff);
        studyRoomMapper.updateById(room);

        // 2. 删除旧时段，重建新时段
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getRoomId, id);
        timeSlotMapper.delete(wrapper);

        saveTimeSlots(id, request.getTimeSlots());

        return toRoomResponse(room);
    }

    /**
     * 删除自习室（事务操作）
     */
    @Override
    @Transactional
    public void deleteRoom(Long id) {
        // 1. 先删时段
        LambdaQueryWrapper<TimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeSlot::getRoomId, id);
        timeSlotMapper.delete(wrapper);

        // 2. 再删自习室
        studyRoomMapper.deleteById(id);
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
                room.getAvailableCapacity(),
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

    /**
     * 保存时段列表
     */
    private void saveTimeSlots(Long roomId, List<RoomRequest.TimeSlotItem> items) {
        for (RoomRequest.TimeSlotItem item : items) {
            TimeSlot slot = new TimeSlot();
            slot.setRoomId(roomId);
            slot.setStartTime(LocalTime.parse(item.getStartTime()));
            slot.setEndTime(LocalTime.parse(item.getEndTime()));
            timeSlotMapper.insert(slot);
        }
    }
}
