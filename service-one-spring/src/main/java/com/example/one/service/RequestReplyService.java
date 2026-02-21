package com.example.one.service;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class RequestReplyService {

    private static final Logger log = LoggerFactory.getLogger(RequestReplyService.class);

    private final JmsTemplate jmsTemplate;
    private final Queue requestQueue;
    private final Queue responseQueue;
    private final ExecutorService senderExecutor;

    public RequestReplyService(
            JmsTemplate jmsTemplate,
            @Qualifier("requestQueue") Queue requestQueue,
            @Qualifier("responseQueue") Queue responseQueue) {
        this.jmsTemplate = jmsTemplate;
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("request-sender-thread");
            t.setDaemon(true);
            return t;
        });
    }

    public String sendAndWaitForResponse(String payload) {
        Future<String> future = senderExecutor.submit(() -> doSendAndReceive(payload));
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for JMS response", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("JMS request/reply failed", e.getCause());
        }
    }

    private String doSendAndReceive(String payload) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Service ONE processing request. correlationId={}, payload={}", correlationId, payload);

        jmsTemplate.send(requestQueue, session -> {
            TextMessage message = session.createTextMessage(payload);
            message.setJMSCorrelationID(correlationId);
            message.setJMSReplyTo(responseQueue);
            return message;
        });

        String selector = "JMSCorrelationID = '" + correlationId + "'";
        Message response = jmsTemplate.receiveSelected(responseQueue, selector);

        if (response == null) {
            throw new IllegalStateException("No response for correlationId=" + correlationId);
        }
        if (!(response instanceof TextMessage)) {
            throw new IllegalStateException("Unsupported response message type: " + response.getClass().getName());
        }

        try {
            String responseText = ((TextMessage) response).getText();
            log.info("Service ONE processed response. correlationId={}, payload={}", correlationId, responseText);
            return responseText;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse text response", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        senderExecutor.shutdownNow();
    }
}
