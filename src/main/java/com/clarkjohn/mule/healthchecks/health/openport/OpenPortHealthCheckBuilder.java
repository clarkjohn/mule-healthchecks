package com.clarkjohn.mule.healthchecks.health.openport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.clarkjohn.mule.healthchecks.HealthCheckBuilder;
import com.codahale.metrics.health.HealthCheck;

/**
 * Build individual {@link OpenPortHealthCheck} based on all registered {@link DefaultHttpRequester}
 * objects within the MuleContext
 *
 * @author john@clarkjohn.com
 */
@Named
public class OpenPortHealthCheckBuilder implements HealthCheckBuilder, MuleContextAware {

    private static Logger LOG = LoggerFactory.getLogger(OpenPortHealthCheckBuilder.class);

    @Value("#{'${mule-healthchecks.openport.hosts:}'.split(',')}")
    private List<String> hosts;

    @Value("${mule-healthchecks.openport.port:0}")
    private int port;

    private MuleContext muleContext;

    @Override
    public Map<String, HealthCheck> getHealthChecks() {

        Map<String, HealthCheck> healthCheckMap = new LinkedHashMap<>();
        try {
            healthCheckMap.putAll(getPortsMuleDefaultHttpRequester());
            healthCheckMap.putAll(getPortsFromProperties());
        } catch (Exception e) {
            LOG.error("Unable to initialize {} healthchecks", OpenPortHealthCheck.class.getSimpleName(), e);
            return new HashMap<String, HealthCheck>();
        }

        return healthCheckMap;
    }

    private Map<String, HealthCheck> getPortsMuleDefaultHttpRequester() {

        Map<String, HealthCheck> healthCheckMap = new LinkedHashMap<>();
        for (Map.Entry<String, DefaultHttpRequester> entry : muleContext.getRegistry().lookupByType(DefaultHttpRequester.class)
                .entrySet()) {

            String host = (entry.getValue().getHost() == null) ? entry.getValue().getConfig().getHost() : entry.getValue().getHost();

            int port = Integer
                    .parseInt((entry.getValue().getPort() == null) ? entry.getValue().getConfig().getPort() : entry.getValue().getPort());

            healthCheckMap.put(OpenPortHealthCheck.class.getSimpleName() + "-" + host + "-" + port,
                    new OpenPortHealthCheck(Arrays.asList(host), port));
        }

        return healthCheckMap;
    }

    private Map<String, HealthCheck> getPortsFromProperties() {

        Map<String, HealthCheck> healthCheckMap = new LinkedHashMap<>();
        if (hosts.size() > 0 && port > 0) {
            healthCheckMap.put(OpenPortHealthCheck.class.getSimpleName() + "-properties", new OpenPortHealthCheck(hosts, port));
        } else {
            LOG.info("Not creating {} from properties; Properties are missing", OpenPortHealthCheck.class.getSimpleName());
        }

        return healthCheckMap;
    }

    @Override
    public void setMuleContext(MuleContext muleContext) {
        this.muleContext = muleContext;
    }

}
