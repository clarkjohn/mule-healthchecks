package com.clarkjohn.mule.healthchecks.health.certexpiration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.transport.ssl.DefaultTlsContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.clarkjohn.mule.healthchecks.HealthCheckBuilder;
import com.codahale.metrics.health.HealthCheck;

/**
 * Build individual {@link CertExpirationHealthCheck} based on all registered
 * {@link DefaultTlsContextFactory} objects within the MuleContext
 *
 * @author john@clarkjohn.com
 */
@Named
public class CertExpirationHealthCheckBuilder implements HealthCheckBuilder, MuleContextAware {

    private static Logger LOG = LoggerFactory.getLogger(CertExpirationHealthCheckBuilder.class);

    @Value("${mule-healthchecks.certexpiration.numberOfdaysToWarnBeforeExpireDate:30}")
    private int numberOfdaysToWarnBeforeExpireDate;

    private MuleContext muleContext;

    @Override
    public Map<String, HealthCheck> getHealthChecks() {

        LOG.info("Creating CertExpirationHealthChecks for numberOfdaysToWarnBeforeExpireDate={}", numberOfdaysToWarnBeforeExpireDate);

        Map<String, HealthCheck> healthCheckMap = new LinkedHashMap<>();
        try {

            for (Map.Entry<String, DefaultTlsContextFactory> entry : muleContext.getRegistry().lookupByType(DefaultTlsContextFactory.class)
                    .entrySet()) {

                healthCheckMap.put(entry.getValue().getTrustStorePath(), new CertExpirationHealthCheck(entry.getValue().getTrustStorePath(),
                        entry.getValue().getTrustStorePassword(), numberOfdaysToWarnBeforeExpireDate));
            }

        } catch (Exception e) {
            LOG.error("Unable to initialize {} healthchecks", CertExpirationHealthCheck.class.getSimpleName(), e);
            return new HashMap<String, HealthCheck>();
        }

        return healthCheckMap;
    }

    @Override
    public void setMuleContext(MuleContext muleContext) {
        this.muleContext = muleContext;
    }

}
