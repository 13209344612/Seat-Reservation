package com.campus.seatreservation.controller;

import com.campus.seatreservation.common.Result;
import com.campus.seatreservation.dto.RoomRequest;
import com.campus.seatreservation.dto.RoomResponse;
import com.campus.seatreservation.service.StudyRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自习室控制器 — 管理自习室的CRUD操作
 *
 * 提供自习室列表查询、详情查看以及管理员的增删改功能。
 * 列表查询使用Redis缓存优化性能。
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;

    /**
     * 获取自习室列表（Redis缓存）
     */
    @GetMapping
    @Cacheable(value = "rooms")
    public Result<List<RoomResponse>> list() {
        return Result.success(studyRoomService.listRooms());
    }

    /**
     * 根据ID获取自习室详情
     */
    @GetMapping("/{id}")
    public Result<RoomResponse> getById(@PathVariable Long id) {
        return Result.success(studyRoomService.getRoomById(id));
    }

    /**
     * 创建自习室（仅管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "rooms", allEntries = true)  // 清除缓存
    public Result<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        return Result.success(studyRoomService.createRoom(request));
    }

    /**
     * 更新自习室（仅管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "rooms", allEntries = true)  // 清除缓存
    public Result<RoomResponse> update(@PathVariable Long id,
                                       @Valid @RequestBody RoomRequest request) {
        return Result.success(studyRoomService.updateRoom(id, request));
    }

    /**
     * 删除自习室（仅管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "rooms", allEntries = true)  // 清除缓存
    public Result<Void> delete(@PathVariable Long id) {
        studyRoomService.deleteRoom(id);
        return Result.success("删除成功");
    }
}
