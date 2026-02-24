package com.example.one.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class RequestReplyService {

    private static final Logger log = LoggerFactory.getLogger(RequestReplyService.class);

    private final JmsTemplate jmsTemplate;
    private final Queue requestQueue;
    private final Queue responseQueue;
    private final String mode;
    private final ExecutorService senderExecutor;

    public RequestReplyService(
            JmsTemplate jmsTemplate,
            @Qualifier("requestQueue") Queue requestQueue,
            @Qualifier("responseQueue") Queue responseQueue,
            @Value("${app.jms.mode:sendAndReceive}") String mode) {
        this.jmsTemplate = jmsTemplate;
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.mode = mode;
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
            throw new java.lang.IllegalStateException("Interrupted while waiting for JMS response", e);
        } catch (ExecutionException e) {
            throw new java.lang.IllegalStateException("JMS request/reply failed", e.getCause());
        }
    }

    private String doSendAndReceive(String payload) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Service ONE processing request. mode={}, correlationId={}, payload={}", mode, correlationId, payload);

        try {
            Message reply;
            if ("selector".equalsIgnoreCase(mode)) {
                reply = sendAndReceiveWithSelector(payload, correlationId);
            } else {
                reply = sendAndReceiveWithTemplate(payload, correlationId);
            }

            if (reply == null) {
                throw new IllegalStateException("No response for correlationId=" + correlationId);
            }

            if (!(reply instanceof TextMessage)) {
                throw new IllegalStateException("Unsupported JMS reply type: " + reply.getClass().getName());
            }

            TextMessage textReply = (TextMessage) reply;
            String replyCorrelationId = textReply.getJMSCorrelationID();
            String response = textReply.getText();
            log.info("Service ONE processed response. correlationId={}, payload={}", correlationId, response);

            if (replyCorrelationId != null && !correlationId.equals(replyCorrelationId)) {
                log.warn(
                        "Service ONE reply correlation mismatch. expected={}, actual={}",
                        correlationId,
                        replyCorrelationId);
            }

            return response;
        } catch (JMSException e) {
            throw new IllegalStateException("Failed to read JMS response", e);
        }
    }

    private Message sendAndReceiveWithTemplate(String payload, String correlationId) {
        return jmsTemplate.sendAndReceive(requestQueue, session -> {
            TextMessage request = session.createTextMessage(payload);
            request.setJMSCorrelationID(correlationId);
            return request;
        });
    }

    private Message sendAndReceiveWithSelector(String payload, String correlationId) {
        jmsTemplate.send(requestQueue, session -> {
            TextMessage request = session.createTextMessage(payload);
            request.setJMSCorrelationID(correlationId);
            return request;
        });

        String selector = "JMSCorrelationID = '" + correlationId + "'";
        return jmsTemplate.receiveSelected(responseQueue, selector);
    }

    @PreDestroy
    public void shutdown() {
        senderExecutor.shutdownNow();
    }
}
