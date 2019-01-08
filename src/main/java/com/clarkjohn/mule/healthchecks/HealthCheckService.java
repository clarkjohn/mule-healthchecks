package com.clarkjohn.mule.healthchecks;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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

    private Instant onceADayNextRunInstant = null;

    @Inject
    private List<HealthCheckBuilder> healthCheckBuilderList;

    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    public void executeHealthChecks() {

        Instant currentInstant = ZonedDateTime.now().toInstant();

        boolean isOnceADayHealthChecksToExecute = isOnceADayHealthChecksToExecute(currentInstant);
        if (isOnceADayHealthChecksToExecute) {
            onceADayNextRunInstant = currentInstant.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        }

        for (String healthCheckName : healthCheckRegistry.getNames()) {
            HealthCheck healthCheck = healthCheckRegistry.getHealthCheck(healthCheckName);
            if (isOnceADayHealthChecksToExecute && healthCheck instanceof MuleHealthCheck
                    && ((MuleHealthCheck) healthCheck).isRunsOnceADay()) {

                LOG.debug("Running a once a day healthcheck");
                execute(healthCheckName, healthCheck);
            } else {
                execute(healthCheckName, healthCheck);
            }
        }
    }

    private boolean isOnceADayHealthChecksToExecute(Instant currentInstant) {

        if (onceADayNextRunInstant == null) {
            // first time running
            return true;
        } else if ((onceADayNextRunInstant != null) && currentInstant.isAfter(onceADayNextRunInstant)) {
            // check if this was run first time today
            return true;
        } else {
            return false;
        }
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
