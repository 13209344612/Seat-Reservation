package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import com.campus.seatreservation.entity.User;
import com.campus.seatreservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public Result<ReserveResponse> reserve(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody ReserveRequest request) {
        return Result.success(reservationService.reserve(user.getId(), request));
    }

    @GetMapping
    public Result<List<ReserveResponse>> list(@AuthenticationPrincipal User user) {
        return Result.success(reservationService.listMyReservations(user.getId()));
    }

    @GetMapping("/{id}")
    public Result<ReserveResponse> getById(@PathVariable Long id) {
        return Result.success(reservationService.getReservationById(id));
    }
}