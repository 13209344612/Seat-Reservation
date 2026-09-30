package com.campus.seatreservation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.seatreservation.entity.Reservation;
import com.campus.seatreservation.entity.TimeSlot;
import com.campus.seatreservation.mapper.ReservationMapper;
import com.campus.seatreservation.mapper.TimeSlotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约超时定时任务
 *
 * 每5分钟扫描一次，将「时段已结束」与「时段已开始超过宽限期仍未签到」的活跃预约置为过期。
 * 判定基于「预约日期 + 时段起止时间」，不会误伤未来日期的预约（根治按 create_time 判定导致的误杀）。
 * 库存为派生计数，过期后自然释放，无需恢复计数器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutTask {

    /** 未签到宽限期：时段开始后 30 分钟仍未签到视为 no-show */
    private static final int NO_SHOW_GRACE_MINUTES = 30;

    private final ReservationMapper reservationMapper;
    private final TimeSlotMapper timeSlotMapper;

    @Scheduled(fixedRate = 300000)
    @Transactional
    @CacheEvict(value = "rooms", allEntries = true)
    public void cancelTimeoutReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 只处理今天及以前的活跃预约：未来日期的预约不可能超时，直接排除
        List<Reservation> activeList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .in(Reservation::getStatus, "booked", "signed")
                        .le(Reservation::getReservationDate, today));
        if (activeList.isEmpty()) {
            return;
        }

        // 批量加载涉及的所有时段，避免逐条 selectById 造成的 N+1
        Set<Long> slotIds = activeList.stream()
                .map(Reservation::getTimeSlotId)
                .collect(Collectors.toSet());
        Map<Long, TimeSlot> slotMap = timeSlotMapper.selectBatchIds(slotIds).stream()
                .collect(Collectors.toMap(TimeSlot::getId, Function.identity()));

        int expiredCount = 0;
        for (Reservation r : activeList) {
            TimeSlot slot = slotMap.get(r.getTimeSlotId());
            if (slot == null) {
                continue;
            }

            LocalDateTime slotStart = r.getReservationDate().atTime(slot.getStartTime());
            LocalDateTime slotEnd = r.getReservationDate().atTime(slot.getEndTime());

            boolean slotEnded = now.isAfter(slotEnd);
            boolean noShow = "booked".equals(r.getStatus())
                    && now.isAfter(slotStart.plusMinutes(NO_SHOW_GRACE_MINUTES));

            if (slotEnded || noShow) {
                // 条件更新，防止与用户签到/取消并发冲突
                LambdaUpdateWrapper<Reservation> update = new LambdaUpdateWrapper<>();
                update.eq(Reservation::getId, r.getId())
                        .in(Reservation::getStatus, "booked", "signed")
                        .set(Reservation::getStatus, "expired");
                if (reservationMapper.update(null, update) > 0) {
                    expiredCount++;
                }
            }
        }
        if (expiredCount > 0) {
            log.info("预约超时处理：置为过期 {} 条", expiredCount);
        }
    }
}
