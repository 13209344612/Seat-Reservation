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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约业务层实现
 *
 * 处理座位预约、签到、取消等核心业务逻辑。
 * 使用分布式锁 + 乐观锁双重保障，防止并发预约时的超卖问题。
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
     * 创建预约（事务操作 + 分布式锁 + 乐观锁）
     * 
     * 使用分布式锁保证同一时刻只有一个请求能处理同一自习室的预约，
     * 避免高并发下的超卖问题。分布式锁作为第一道防线，乐观锁作为第二道防线。
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

            // 2. 检查同一用户是否已预约相同时段
            LambdaQueryWrapper<Reservation> dupCheck = new LambdaQueryWrapper<>();
            dupCheck.eq(Reservation::getUserId, userId)
                    .eq(Reservation::getRoomId, request.getRoomId())
                    .eq(Reservation::getTimeSlotId, request.getTimeSlotId())
                    .eq(Reservation::getReservationDate, request.getReservationDate())
                    .ne(Reservation::getStatus, "cancelled");
            if (reservationMapper.selectCount(dupCheck) > 0) {
                throw new RuntimeException("您已预约过该时段，请勿重复预约");
            }

            // 3. 乐观锁扣减库存（原子操作）
            // 分布式锁已经保证了并发安全，这里乐观锁作为第二道防线
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

            // 4. 插入预约记录
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setRoomId(request.getRoomId());
            reservation.setTimeSlotId(request.getTimeSlotId());
            reservation.setReservationDate(request.getReservationDate());
            reservation.setStatus("booked");
            reservationMapper.insert(reservation);

            log.info("用户 {} 预约成功，预约ID: {}", userId, reservation.getId());

            // 5. 组装响应
            User user = userMapper.selectById(userId);
            // 构建短信消息
            SmsMessage smsMsg = new SmsMessage(
                    reservation.getId(),
                    userId,
                    user.getPhone(),            // User 实体需加 phone 字段
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
