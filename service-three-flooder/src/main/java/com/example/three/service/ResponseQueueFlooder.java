package com.example.three.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.UUID;

@Component
public class ResponseQueueFlooder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ResponseQueueFlooder.class);

    private final JmsTemplate jmsTemplate;
    private final Queue responseQueue;
    private final ConfigurableApplicationContext context;

    @Value("${flooder.messages:7000}")
    private int messagesToSend;

    public ResponseQueueFlooder(JmsTemplate jmsTemplate, Queue responseQueue, ConfigurableApplicationContext context) {
        this.jmsTemplate = jmsTemplate;
        this.responseQueue = responseQueue;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Service THREE flood start. queue=one.response, messages={}", messagesToSend);

        for (int i = 1; i <= messagesToSend; i++) {
            String correlationId = UUID.randomUUID().toString();
            String payload = "flood-message-" + i;

            jmsTemplate.send(responseQueue, session -> {
                TextMessage message = session.createTextMessage(payload);
                message.setJMSCorrelationID(correlationId);
                return message;
            });

            if (i % 500 == 0 || i == messagesToSend) {
                log.info("Service THREE flood progress. sent={}/{}", i, messagesToSend);
            }
        }

        log.info("Service THREE flood done. sent={} messages", messagesToSend);
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}
