package com.campus.seatreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.seatreservation.config.RabbitMQConfig;
import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.dto.SmsMessage;
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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约业务层实现
 *
 * 处理座位预约、签到、取消等核心业务逻辑。
 * 库存为派生计数：按「房间+日期+时段」维度实时统计活跃预约数，无房间级计数器。
 * 并发防超卖由 room+date+slot 维度的 Redisson 分布式锁 + 锁内派生计数校验 +
 * 唯一索引 uk_active_reservation 三层保障。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final UserMapper userMapper;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;

    /**
     * 创建预约（事务操作 + 分布式锁 + 派生计数校验）
     *
     * 分布式锁按 room+date+slot 维度串行化，锁内统计活跃预约数与总容量比较做余量校验，
     * 唯一索引 uk_active_reservation 作为兜底防线，共同防止高并发下的超卖与重复预约。
     */
    @Override
    @Transactional
    public ReserveResponse reserve(Long userId, ReserveRequest request) {
        // 分布式锁的 key：基于自习室ID + 日期 + 时段ID，确保同一资源的并发控制
        String lockKey = String.format("reservation:lock:%d:%s:%d", 
                request.getRoomId(), 
                request.getReservationDate(), 
                request.getTimeSlotId());
        
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁：最多等待3秒，获取到锁后持有10秒自动释放
            // 看门狗机制会自动续期，防止业务执行时间过长导致锁过期
            boolean locked = lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!locked) {
                log.warn("用户 {} 预约自习室 {} 失败：系统繁忙，请稍后重试", userId, request.getRoomId());
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            
            log.info("用户 {} 成功获取分布式锁，开始预约自习室 {}", userId, request.getRoomId());

            // 1. 校验自习室和时段是否存在
            StudyRoom room = studyRoomMapper.selectById(request.getRoomId());
            if (room == null) {
                throw new RuntimeException("自习室不存在");
            }

            TimeSlot slot = timeSlotMapper.selectById(request.getTimeSlotId());
            if (slot == null || !slot.getRoomId().equals(request.getRoomId())) {
                throw new RuntimeException("该时段不属于此自习室");
            }

            // 2. 日期校验：只能预约今天或未来
            if (request.getReservationDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("不能预约过去的日期");
            }

            // 3. 重复校验：同一用户对同一(房间+时段+日期)只能有一个活跃预约
            LambdaQueryWrapper<Reservation> dupCheck = new LambdaQueryWrapper<>();
            dupCheck.eq(Reservation::getUserId, userId)
                    .eq(Reservation::getRoomId, request.getRoomId())
                    .eq(Reservation::getTimeSlotId, request.getTimeSlotId())
                    .eq(Reservation::getReservationDate, request.getReservationDate())
                    .in(Reservation::getStatus, "booked", "signed");
            if (reservationMapper.selectCount(dupCheck) > 0) {
                throw new RuntimeException("您已预约过该时段，请勿重复预约");
            }

            // 4. 派生计数校验余量：统计该(房间+时段+日期)下的活跃预约数
            //    分布式锁已按 room+date+slot 维度串行化，此处计数不存在竞态
            LambdaQueryWrapper<Reservation> capacityCheck = new LambdaQueryWrapper<>();
            capacityCheck.eq(Reservation::getRoomId, request.getRoomId())
                    .eq(Reservation::getTimeSlotId, request.getTimeSlotId())
                    .eq(Reservation::getReservationDate, request.getReservationDate())
                    .in(Reservation::getStatus, "booked", "signed");
            long bookedCount = reservationMapper.selectCount(capacityCheck);
            if (bookedCount >= room.getTotalCapacity()) {
                throw new RuntimeException("该时段名额已满，请重试");
            }

            // 5. 插入预约记录（唯一索引 uk_active_reservation 作为兜底防线）
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setRoomId(request.getRoomId());
            reservation.setTimeSlotId(request.getTimeSlotId());
            reservation.setReservationDate(request.getReservationDate());
            reservation.setStatus("booked");
            try {
                reservationMapper.insert(reservation);
            } catch (DuplicateKeyException e) {
                throw new RuntimeException("您已预约过该时段，请勿重复预约");
            }

            log.info("用户 {} 预约成功，预约ID: {}", userId, reservation.getId());

            // 6. 组装响应
            User user = userMapper.selectById(userId);
            // 构建短信消息
            SmsMessage smsMsg = new SmsMessage(
                    reservation.getId(),
                    userId,
                    user.getPhone(),
                    user.getUsername(),
                    room.getName(),
                    slot.getStartTime() + "-" + slot.getEndTime(),
                    request.getReservationDate()
            );

            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(RabbitMQConfig.SMS_QUEUE, smsMsg);
            }

            return toReserveResponse(reservation, user, room, slot);
            
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            log.error("用户 {} 预约被中断", userId, e);
            throw new RuntimeException("预约被中断");
        } finally {
            // 释放锁：只释放当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("用户 {} 释放分布式锁", userId);
            }
        }
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
     * 取消预约（事务操作）
     *
     * 库存为派生计数，取消后活跃预约数自然减少即释放余量，无需恢复计数器。
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

        // 条件更新：仅当仍为 booked 时才置为 cancelled，防止并发重复取消
        // 库存为派生计数，取消后自然释放，无需恢复计数器
        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .eq(Reservation::getStatus, "booked")
                .set(Reservation::getStatus, "cancelled");
        int affected = reservationMapper.update(null, updateWrapper);
        if (affected == 0) {
            throw new RuntimeException("当前状态不可取消");
        }
        reservation.setStatus("cancelled");

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
