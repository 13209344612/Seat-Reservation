package com.campus.seatreservation.service.impl;

import com.campus.seatreservation.config.RabbitMQConfig;
import com.campus.seatreservation.dto.SmsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;

@Slf4j
@Service
@ConditionalOnClass(RabbitListener.class)
public class SmsConsumer {

    /**
     * 监听 reservation.sms 队列
     * 收到消息后模拟调第三方短信 API 发送
     * 
     * 添加手动 ACK 机制：
     * - 成功处理 → basicAck（确认消费）
     * - 处理失败 → basicNack（拒绝并重新入队，防止消息丢失）
     */
    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void sendSms(SmsMessage msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("[短信] 开始发送 → 手机={} 内容='{}，您预约的 {}-{} {} 已确认'",
                    msg.getPhoneNumber(), msg.getNickName(),
                    msg.getRoomName(), msg.getDate(), msg.getTimeRange());

            // ========== 模拟短信 API 调用耗时 ==========
            // 真实场景替换为：
            //   AliyunSmsClient.send(phone, templateCode, params)
            // 网络请求 + 运营商下发耗时约 1-3 秒
            Thread.sleep(2000);

            log.info("[短信] 发送成功 → 手机={}", msg.getPhoneNumber());
            
            // 手动确认消息（成功）
            channel.basicAck(tag, false);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[短信] 发送被中断 → 手机={}", msg.getPhoneNumber());
            try {
                // 拒绝消息，不重新入队（避免无限重试）
                channel.basicNack(tag, false, false);
            } catch (Exception ex) {
                log.error("[短信] 拒绝消息失败", ex);
            }
        } catch (Exception e) {
            log.error("[短信] 发送失败 → 手机={}, 错误: {}", msg.getPhoneNumber(), e.getMessage());
            try {
                // 拒绝消息并重新入队（可重试的错误，如网络超时）
                channel.basicNack(tag, false, true);
            } catch (Exception ex) {
                log.error("[短信] 拒绝消息失败", ex);
            }
        }
    }
}