package com.campus.seatreservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.seatreservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
