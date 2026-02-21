package com.example.one.config;

import org.apache.activemq.broker.BrokerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedBrokerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BrokerService brokerService() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName("embedded-broker");
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://0.0.0.0:61616");
        return broker;
    }
}
