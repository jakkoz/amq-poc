package com.example.two.routes;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class RequestResponseRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("jms:queue:one.request")
                .routeId("one-request-response-route")
                .log("Service TWO processed request. correlationId=${header.JMSCorrelationID}, payload=${body}")
                .delay(15)
                .setBody(simple("processed by camel: ${body}"))
                .setHeader("JMSCorrelationID", simple("${header.JMSCorrelationID}"))
                .removeHeader("JMSReplyTo")
                .log("Service TWO processed response. correlationId=${header.JMSCorrelationID}, payload=${body}")
                .setExchangePattern(ExchangePattern.InOnly)
                .to("jms:queue:one.response?exchangePattern=InOnly");
    }
}
