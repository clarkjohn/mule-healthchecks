package com.clarkjohn.mule.healthchecks;

import java.util.Map;

import com.codahale.metrics.health.HealthCheck;

/**
 *
 * Create a map of healthcheck name to {@link HealthCheck} for use within the
 * {@link HealthCheckService}
 *
 * @author john@clarkjohn.com
 *
 */
public interface HealthCheckBuilder {

    public Map<String, ? extends HealthCheck> getHealthChecks();
}
