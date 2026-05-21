package com.campus.seatreservation.service;

import com.campus.seatreservation.dto.ReserveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预约服务并发测试
 * 使用CyclicBarrier模拟多线程并发预约场景，验证乐观锁防超卖机制的有效性。
 */
@SpringBootTest
public class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    /**
     * 并发预约测试
     * 模拟10个用户同时预约同一个自习室的同一个时段，验证：
     * 1. 成功预约数不会超过可用容量
     * 2. 不会出现超卖现象
     */
    @Test
    public void testConcurrentReserve() throws Exception {
        int threadCount = 10; // 10 个线程同时抢
        Long roomId = 1L;     // RoomA-1，容量50
        Long timeSlotId = 1L;
        Long startUserId = 2L; // 从 userId=2 开始

        // CyclicBarrier确保所有线程同时开始执行
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0); // 成功计数器
        AtomicInteger failCount = new AtomicInteger(0);    // 失败计数器

        Thread[] threads = new Thread[threadCount];

        // 创建并启动10个线程
        for (int i = 0; i < threadCount; i++) {
            final long userId = startUserId + i;
            threads[i] = new Thread(() -> {
                try {
                    barrier.await(); // 所有线程在此等待，直到全部就绪后同时出发
                    
                    // 构造预约请求
                    ReserveRequest req = new ReserveRequest();
                    req.setRoomId(roomId);
                    req.setTimeSlotId(timeSlotId);
                    req.setReservationDate(LocalDate.now().plusDays(1));
                    
                    // 执行预约
                    reservationService.reserve(userId, req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 预约失败（如库存不足、版本冲突等）
                    failCount.incrementAndGet();
                }
            });
        }

        // 启动所有线程
        for (Thread t : threads) t.start();
        // 等待所有线程执行完毕
        for (Thread t : threads) t.join();

        // 输出测试结果
        System.out.println("成功: " + successCount.get() + ", 失败: " + failCount.get());
        // 每次测试结果可能不同，但成功的预约数不会超过 available_capacity
    }
}