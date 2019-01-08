package com.clarkjohn.mule.healthchecks;

import com.codahale.metrics.health.HealthCheck;

public abstract class MuleHealthCheck extends HealthCheck {

    public abstract boolean isRunsOnceADay();
}
