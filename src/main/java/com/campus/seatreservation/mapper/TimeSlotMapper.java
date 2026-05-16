package com.campus.seatreservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.seatreservation.entity.TimeSlot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlot> {
}