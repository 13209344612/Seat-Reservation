package com.campus.seatreservation.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 *
 * 配置 Redisson 客户端，用于实现分布式锁功能。
 * 在并发预约场景下，分布式锁可以有效防止超卖问题。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private Integer redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 创建 Redisson 客户端 Bean
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 构建 Redis 地址
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        
        // 配置单机模式
        config.useSingleServer()
                .setAddress(address)
                .setConnectionPoolSize(10)           // 连接池大小
                .setConnectionMinimumIdleSize(5)     // 最小空闲连接数
                .setConnectTimeout(10000)            // 连接超时时间（毫秒）
                .setTimeout(3000);                   // 命令执行超时时间（毫秒）
        
        // 如果有密码，设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
