package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.ReserveRequest;
import com.campus.seatreservation.dto.ReserveResponse;
import java.util.List;

/**
 * 预约业务层接口
 */
public interface ReservationService {
    ReserveResponse reserve(Long userId, ReserveRequest request);
    List<ReserveResponse> listMyReservations(Long userId);
    ReserveResponse getReservationById(Long reservationId);
    ReserveResponse sign(Long reservationId, Long userId);
    ReserveResponse cancel(Long reservationId, Long userId);
}
