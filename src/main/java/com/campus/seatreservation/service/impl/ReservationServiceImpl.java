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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约业务层实现
 *
 * 处理座位预约、签到、取消等核心业务逻辑。
 * 使用乐观锁防止并发预约时的超卖问题。
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final UserMapper userMapper;

    /**
     * 创建预约（事务操作 + 乐观锁）
     */
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

        // 2. 乐观锁扣减库存（原子操作）
        LambdaUpdateWrapper<StudyRoom> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StudyRoom::getId, request.getRoomId())
                .gt(StudyRoom::getAvailableCapacity, 0)  // 确保还有余量
                .eq(StudyRoom::getVersion, room.getVersion())  // 版本号匹配
                .setSql("available_capacity = available_capacity - 1")  // 原子扣减
                .setSql("version = version + 1");  // 版本号递增

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
    /**
     * 获取我的预约列表
     */
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

    /**
     * 根据ID获取预约详情
     */
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

    /**
     * 签到（事务操作）
     */
    @Override
    @Transactional
    public ReserveResponse sign(Long reservationId, Long userId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("只能签到自己的预约");
        }
        if (!"booked".equals(reservation.getStatus())) {
            throw new RuntimeException("当前状态不可签到：" + reservation.getStatus());
        }

        // 更新状态为已签到，记录签到时间
        reservation.setStatus("signed");
        reservation.setSignTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        return toReserveResponse(reservation,
                userMapper.selectById(userId),
                studyRoomMapper.selectById(reservation.getRoomId()),
                timeSlotMapper.selectById(reservation.getTimeSlotId()));
    }

    /**
     * 取消预约（事务操作 + 恢复库存）
     */
    @Override
    @Transactional
    public ReserveResponse cancel(Long reservationId, Long userId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("只能取消自己的预约");
        }
        if (!"booked".equals(reservation.getStatus())) {
            throw new RuntimeException("当前状态不可取消：" + reservation.getStatus());
        }

        // 1. 更新预约状态为已取消
        reservation.setStatus("cancelled");
        reservationMapper.updateById(reservation);

        // 2. 恢复库存（原子操作）
        LambdaUpdateWrapper<StudyRoom> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StudyRoom::getId, reservation.getRoomId())
                .setSql("available_capacity = available_capacity + 1");

        studyRoomMapper.update(null, updateWrapper);

        return toReserveResponse(reservation,
                userMapper.selectById(userId),
                studyRoomMapper.selectById(reservation.getRoomId()),
                timeSlotMapper.selectById(reservation.getTimeSlotId()));
    }

    /**
     * 将预约实体转换为响应DTO
     */
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
