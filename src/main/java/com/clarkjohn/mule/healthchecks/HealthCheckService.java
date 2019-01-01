package com.clarkjohn.mule.healthchecks;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Service to execute all Mule {@link healthCheck}(s)
 *
 * @author john@clarkjohn.com
 */
@Named
public class HealthCheckService {

    private static Logger LOG = LoggerFactory.getLogger(HealthCheckService.class);

    @Inject
    private List<HealthCheckBuilder> healthCheckBuilderList;

    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    public void executeHealthChecks() {
        healthCheckRegistry.getNames().forEach(name -> execute(name, healthCheckRegistry.getHealthCheck(name)));
    }

    private void execute(String healthCheckName, HealthCheck healthCheck) {

        Result result = healthCheck.execute();
        if (result.isHealthy()) {
            LOG.info("Healthy {} for {}; Result={}", healthCheck.getClass().getSimpleName(), healthCheckName, result.getMessage());
        } else {
            LOG.warn("Unhealthy {} for {}; Result={}", healthCheck.getClass().getSimpleName(), healthCheckName, result.getMessage());
        }
    }

    @PostConstruct
    public void initHealthCheckService() {

        LOG.info("Registering healthchecks");
        healthCheckBuilderList
                .forEach(heathCheckBuilder -> heathCheckBuilder.getHealthChecks().forEach((k, v) -> healthCheckRegistry.register(k, v)));

        LOG.info("Registered the following healthchecks={}", healthCheckRegistry.getNames());
    }

}
