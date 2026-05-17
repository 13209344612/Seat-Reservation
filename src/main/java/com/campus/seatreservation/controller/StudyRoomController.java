package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.service.StudyRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    @GetMapping
    public Result<List<RoomResponse>> list() {
        return Result.success(studyRoomService.listRooms());
    }

    @GetMapping("/{id}")
    public Result<RoomResponse> getById(@PathVariable Long id) {
        return Result.success(studyRoomService.getRoomById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        return Result.success(studyRoomService.createRoom(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RoomResponse> update(@PathVariable Long id,
                                       @Valid @RequestBody RoomRequest request) {
        return Result.success(studyRoomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        studyRoomService.deleteRoom(id);
        return Result.success("删除成功");
    }
}
