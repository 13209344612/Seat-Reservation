package com.campus.seatreservation.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Queue.class)
public class RabbitMQConfig {
    public static final String SMS_QUEUE = "reservation.sms";

    @Bean
    public Queue smsQueue() {
        return new Queue(SMS_QUEUE, true);
    }
}
