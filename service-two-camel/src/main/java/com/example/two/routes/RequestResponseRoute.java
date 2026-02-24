package com.example.two.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.ExchangePattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestResponseRoute extends RouteBuilder {

    @Value("${app.jms.mode:sendAndReceive}")
    private String mode;

    @Override
    public void configure() {
        if ("selector".equalsIgnoreCase(mode)) {
            from("jms:queue:one.request?exchangePattern=InOnly")
                    .routeId("one-request-response-route-selector")
                    .log("Service TWO processed request. correlationId=${header.JMSCorrelationID}, payload=${body}")
//                    .delay(15)
                    .setBody(simple("processed by camel: ${body}"))
                    .setHeader("JMSCorrelationID", simple("${header.JMSCorrelationID}"))
                    .setExchangePattern(ExchangePattern.InOnly)
                    .to("jms:queue:one.response?exchangePattern=InOnly")
                    .log("Service TWO processed response. correlationId=${header.JMSCorrelationID}, payload=${body}");
            return;
        }

        from("jms:queue:one.request?exchangePattern=InOut")
                .routeId("one-request-response-route-sendandreceive")
                .log("Service TWO processed request. correlationId=${header.JMSCorrelationID}, payload=${body}")
                .delay(15)
                .setBody(simple("processed by camel: ${body}"))
                .setHeader("JMSCorrelationID", simple("${header.JMSCorrelationID}"))
                .log("Service TWO processed response. correlationId=${header.JMSCorrelationID}, payload=${body}");
    }
}
