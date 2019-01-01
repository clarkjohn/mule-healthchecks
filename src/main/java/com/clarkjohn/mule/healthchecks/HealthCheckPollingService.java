package com.clarkjohn.mule.healthchecks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Poll {@link HealthCheckService} from within within a Mule Flow
 *
 * @author john@clarkjohn.com
 */
public class HealthCheckPollingService implements Callable {

    private static Logger LOG = LoggerFactory.getLogger(HealthCheckPollingService.class);

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Inject
    private HealthCheckService healthCheckService;

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {

        if (executor != null) {
            runHealthcheckExecuterService();
        } else {
            LOG.warn("Executer service  is null, stopping " + HealthCheckPollingService.class.getSimpleName());
        }

        return eventContext.getMessage();
    }

    private void runHealthcheckExecuterService() {

        LOG.info("Starting " + HealthCheckPollingService.class.getSimpleName());

        Future<Void> future = executor.submit(new Task());
        try {
            // polls every hour, this will timeout any current healthchecks. See mule-healthchecks.xml
            future.get(58, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            LOG.error("HealthChecks timed out", e);
            future.cancel(true);
        } catch (Exception e) {
            LOG.error("Exception running " + HealthCheckPollingService.class.getSimpleName(), e);
            future.cancel(true);
        }

        LOG.info("Completed " + HealthCheckPollingService.class.getSimpleName());
    }

    class Task implements java.util.concurrent.Callable<Void> {

        @Override
        public Void call() throws Exception {

            healthCheckService.executeHealthChecks();

            return null;
        }
    }
}
