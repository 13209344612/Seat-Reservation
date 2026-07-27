package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约控制器 — 管理座位预约相关操作
 *
 * 提供预约创建、查询、签到和取消功能。
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 创建预约
     */
    @PostMapping
    @CacheEvict(value = "rooms", allEntries = true)
    public Result<ReserveResponse> reserve(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody ReserveRequest request) {
        return Result.success(reservationService.reserve(user.getId(), request));
    }

    /**
     * 获取我的预约列表
     */
    @GetMapping
    public Result<List<ReserveResponse>> list(@AuthenticationPrincipal User user) {
        return Result.success(reservationService.listMyReservations(user.getId()));
    }

    /**
     * 根据ID获取预约详情
     */
    @GetMapping("/{id}")
    public Result<ReserveResponse> getById(@PathVariable Long id) {
        return Result.success(reservationService.getReservationById(id));
    }

    /**
     * 签到
     */
    @PostMapping("/{id}/sign")
    public Result<ReserveResponse> sign(@PathVariable Long id,
                                        @AuthenticationPrincipal User user) {
        return Result.success(reservationService.sign(id, user.getId()));
    }

    /**
     * 取消预约
     */
    @PostMapping("/{id}/cancel")
    @CacheEvict(value = "rooms", allEntries = true)
    public Result<ReserveResponse> cancel(@PathVariable Long id,
                                          @AuthenticationPrincipal User user) {
        return Result.success(reservationService.cancel(id, user.getId()));
    }
}