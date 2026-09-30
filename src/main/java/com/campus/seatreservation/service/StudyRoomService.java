package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.dto.SlotAvailability;

import java.time.LocalDate;
import java.util.List;

/**
 * 自习室业务层接口
 */
public interface StudyRoomService {
    List<RoomResponse> listRooms();
    RoomResponse getRoomById(Long id);
    RoomResponse createRoom(RoomRequest request);
    RoomResponse updateRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);

    /** 查询某自习室在指定日期下各时段的座位余量 */
    List<SlotAvailability> getAvailability(Long roomId, LocalDate date);
}
