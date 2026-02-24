package com.example.one.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

@Configuration
public class JmsConfig {

    @Bean(destroyMethod = "stop")
    @Primary
    public ConnectionFactory pooledConnectionFactory(
            @Value("${spring.activemq.broker-url}") String brokerUrl,
            @Value("${spring.activemq.user}") String user,
            @Value("${spring.activemq.password}") String password) {
        ActiveMQConnectionFactory target = new ActiveMQConnectionFactory(user, password, brokerUrl);

        PooledConnectionFactory pool = new PooledConnectionFactory();
        pool.setConnectionFactory(target);
        pool.setMaxConnections(1);
        return pool;
    }

    @Bean
    public Queue requestQueue() {
        return new ActiveMQQueue("one.request");
    }

    @Bean
    public Queue responseQueue() {
        return new ActiveMQQueue("one.response");
    }
}
