package com.campus.seatreservation.service.impl;

import com.campus.seatreservation.config.RabbitMQConfig;
import com.campus.seatreservation.dto.SmsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnClass(RabbitListener.class)
public class SmsConsumer {

    /**
     * 监听 reservation.sms 队列
     * 收到消息后模拟调第三方短信 API 发送
     */
    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void sendSms(SmsMessage msg) {
        log.info("[短信] 开始发送 → 手机={} 内容='{}，您预约的 {}-{} {} 已确认'",
                msg.getPhoneNumber(), msg.getNickName(),
                msg.getRoomName(), msg.getDate(), msg.getTimeRange());

        // ========== 模拟短信 API 调用耗时 ==========
        // 真实场景替换为：
        //   AliyunSmsClient.send(phone, templateCode, params)
        // 网络请求 + 运营商下发耗时约 1-3 秒
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[短信] 发送被中断");
            return;
        }

        log.info("[短信] 发送成功 → 手机={}", msg.getPhoneNumber());
    }
}