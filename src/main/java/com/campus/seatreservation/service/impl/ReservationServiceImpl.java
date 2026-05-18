package com.campus.seatreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.entity.Reservation;
import com.campus.seatreservation.entity.StudyRoom;
import com.campus.seatreservation.entity.TimeSlot;
import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.mapper.ReservationMapper;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import com.campus.seatreservation.mapper.TimeSlotMapper;
import com.campus.seatreservation.mapper.UserMapper;
import com.campus.seatreservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final UserMapper userMapper;
    @Override
    @Transactional
    public ReserveResponse reserve(Long userId, ReserveRequest request) {
        // 1. 校验自习室和时段是否存在
        StudyRoom room = studyRoomMapper.selectById(request.getRoomId());
        if (room == null) {
            throw new RuntimeException("自习室不存在");
        }

        TimeSlot slot = timeSlotMapper.selectById(request.getTimeSlotId());
        if (slot == null || !slot.getRoomId().equals(request.getRoomId())) {
            throw new RuntimeException("该时段不属于此自习室");
        }

        // 2. 乐观锁扣减库存
        LambdaUpdateWrapper<StudyRoom> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StudyRoom::getId, request.getRoomId())
                .gt(StudyRoom::getAvailableCapacity, 0)
                .eq(StudyRoom::getVersion, room.getVersion())
                .setSql("available_capacity = available_capacity - 1")
                .setSql("version = version + 1");

        int affected = studyRoomMapper.update(null, updateWrapper);
        if (affected == 0) {
            throw new RuntimeException("该时段名额已满，请重试");
        }

        // 3. 插入预约记录
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setRoomId(request.getRoomId());
        reservation.setTimeSlotId(request.getTimeSlotId());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setStatus("booked");
        reservationMapper.insert(reservation);

        // 4. 组装响应
        User user = userMapper.selectById(userId);
        return toReserveResponse(reservation, user, room, slot);
    }
    @Override
    public List<ReserveResponse> listMyReservations(Long userId) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime);
        List<Reservation> reservations = reservationMapper.selectList(wrapper);

        return reservations.stream()
                .map(r -> toReserveResponse(r,
                        userMapper.selectById(r.getUserId()),
                        studyRoomMapper.selectById(r.getRoomId()),
                        timeSlotMapper.selectById(r.getTimeSlotId())))
                .collect(Collectors.toList());
    }
    @Override
    public ReserveResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        return toReserveResponse(reservation,
                userMapper.selectById(reservation.getUserId()),
                studyRoomMapper.selectById(reservation.getRoomId()),
                timeSlotMapper.selectById(reservation.getTimeSlotId()));
    }
    private ReserveResponse toReserveResponse(Reservation r, User u, StudyRoom room, TimeSlot slot) {
        return new ReserveResponse(
                r.getId(),
                r.getUserId(),
                u != null ? u.getUsername() : null,
                r.getRoomId(),
                room != null ? room.getName() : null,
                r.getTimeSlotId(),
                slot != null ? slot.getStartTime() : null,
                slot != null ? slot.getEndTime() : null,
                r.getReservationDate(),
                r.getStatus(),
                r.getSignTime(),
                r.getCreateTime()
        );
    }
}
