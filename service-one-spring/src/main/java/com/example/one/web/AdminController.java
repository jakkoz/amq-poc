package com.example.one.web;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class AdminController {

    private final BrokerService brokerService;

    public AdminController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @GetMapping("/api/admin/queues")
    public Map<String, Object> queues() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brokerName", brokerService.getBrokerName());
        result.put("one.request", queueStats("one.request"));
        result.put("one.response", queueStats("one.response"));
        return result;
    }

    private Map<String, Object> queueStats(String queueName) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName query = new ObjectName(
                "org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*");

        Map<String, Object> stats = new LinkedHashMap<>();
        ObjectName objectName = null;
        for (ObjectName candidate : mBeanServer.queryNames(query, null)) {
            String destinationName = candidate.getKeyProperty("destinationName");
            if (queueName.equals(destinationName)) {
                objectName = candidate;
                break;
            }
        }

        if (objectName == null) {
            stats.put("registered", false);
            return stats;
        }

        QueueViewMBean proxy = javax.management.JMX.newMBeanProxy(
                mBeanServer,
                objectName,
                QueueViewMBean.class);

        stats.put("registered", true);
        stats.put("queueSize", proxy.getQueueSize());
        stats.put("enqueueCount", proxy.getEnqueueCount());
        stats.put("dequeueCount", proxy.getDequeueCount());
        stats.put("dispatchCount", proxy.getDispatchCount());
        stats.put("inFlightCount", proxy.getInFlightCount());
        stats.put("consumerCount", proxy.getConsumerCount());
        return stats;
    }
}
