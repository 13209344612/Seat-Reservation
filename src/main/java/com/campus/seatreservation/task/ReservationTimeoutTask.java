package com.campus.seatreservation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.seatreservation.entity.Reservation;
import com.campus.seatreservation.entity.StudyRoom;
import com.campus.seatreservation.mapper.ReservationMapper;
import com.campus.seatreservation.mapper.StudyRoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutTask {

    private final ReservationMapper reservationMapper;
    private final StudyRoomMapper studyRoomMapper;

    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    @Transactional
    public void cancelTimeoutReservations() {
        // 1. 查超时预约：状态=booked 且 创建超过30分钟未签到
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        LambdaQueryWrapper<Reservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Reservation::getStatus, "booked")
                .lt(Reservation::getCreateTime, deadline);

        List<Reservation> timeoutList = reservationMapper.selectList(queryWrapper);

        if (timeoutList.isEmpty()) {
            return;
        }

        // 2. 逐个取消 + 恢复库存
        for (Reservation r : timeoutList) {
            r.setStatus("cancelled");
            reservationMapper.updateById(r);

            LambdaUpdateWrapper<StudyRoom> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(StudyRoom::getId, r.getRoomId())
                    .setSql("available_capacity = available_capacity + 1");
            studyRoomMapper.update(null, updateWrapper);
        }

        log.info("定时回收完成：取消 {} 条超时预约", timeoutList.size());
    }
}