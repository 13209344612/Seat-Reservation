package com.campus.seatreservation.controller.internal;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内部预约接口 —— 仅供 Python Agent 服务通过服务间鉴权调用（非对外 /api）。
 *
 * 与对外 ReservationController 的关键区别：userId 不由 @AuthenticationPrincipal 注入，
 * 而是由受信调用方（Agent 在校验用户 JWT 后）显式传参。信任边界由 X-Internal-Token 保证：
 * 只有持有内部 token 的 Agent 能调用，Agent 侧确保 userId 来自真实登录用户，杜绝越权。
 *
 * 写操作（创建/取消）与对外接口一致地清除 rooms 缓存，保证座位余量实时刷新。
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalReservationController {

    private final ReservationService reservationService;

    /** 查询指定用户的全部预约记录。 */
    @GetMapping("/users/{userId}/reservations")
    public Result<List<ReserveResponse>> listByUser(@PathVariable Long userId) {
        return Result.success(reservationService.listMyReservations(userId));
    }

    /** 为指定用户创建预约。 */
    @PostMapping("/reservations")
    @CacheEvict(value = "rooms", allEntries = true)
    public Result<ReserveResponse> reserve(@RequestParam Long userId,
                                           @Valid @RequestBody ReserveRequest request) {
        return Result.success(reservationService.reserve(userId, request));
    }

    /** 取消指定用户的某个预约。 */
    @PostMapping("/reservations/{id}/cancel")
    @CacheEvict(value = "rooms", allEntries = true)
    public Result<ReserveResponse> cancel(@PathVariable Long id,
                                          @RequestParam Long userId) {
        return Result.success(reservationService.cancel(id, userId));
    }
}
