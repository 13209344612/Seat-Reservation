package com.campus.seatreservation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.seatreservation.entity.Reservation;
import com.campus.seatreservation.entity.StudyRoom;
import com.campus.seatreservation.entity.TimeSlot;
import com.campus.seatreservation.mapper.ReservationMapper;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import com.campus.seatreservation.mapper.TimeSlotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约超时定时任务
 * 每5分钟扫描一次数据库，自动取消超过30分钟未签到的预约，并恢复库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutTask {

    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper studyRoomMapper;
    private final TimeSlotMapper timeSlotMapper;

    /**
     * 定时回收预约
     * 每5分钟执行一次：
     * 1. 已预约但30分钟未签到 → 取消 + 恢复库存
     * 2. 已签到/已预约但时段已结束 → 过期 + 恢复库存
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public void cancelTimeoutReservations() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 超时未签到：booked 超过30分钟 → 取消
        LocalDateTime signDeadline = now.minusMinutes(30);
        List<Reservation> timeoutList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, "booked")
                        .lt(Reservation::getCreateTime, signDeadline));

        int count = 0;
        for (Reservation r : timeoutList) {
            r.setStatus("cancelled");
            reservationMapper.updateById(r);
            restoreCapacity(r.getRoomId());
            count++;
        }
        if (count > 0) {
            log.info("超时未签到：取消 {} 条预约", count);
        }

        // 2. 时段已结束：booked/signed 但 reservation_date + end_time 已过 → 过期释放
        List<Reservation> activeList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .in(Reservation::getStatus, "booked", "signed"));

        int expiredCount = 0;
        for (Reservation r : activeList) {
            TimeSlot slot = timeSlotMapper.selectById(r.getTimeSlotId());
            if (slot == null) continue;

            LocalDateTime slotEnd = r.getReservationDate().atTime(slot.getEndTime());
            if (now.isAfter(slotEnd)) {
                r.setStatus("expired");
                reservationMapper.updateById(r);
                restoreCapacity(r.getRoomId());
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("时段结束：过期 {} 条预约", expiredCount);
        }
    }

    private void restoreCapacity(Long roomId) {
        LambdaUpdateWrapper<StudyRoom> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StudyRoom::getId, roomId)
                .setSql("available_capacity = available_capacity + 1");
        studyRoomMapper.update(null, updateWrapper);
    }
}