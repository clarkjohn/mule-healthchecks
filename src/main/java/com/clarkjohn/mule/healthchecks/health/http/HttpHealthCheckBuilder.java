package com.clarkjohn.mule.healthchecks.health.http;

import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.construct.Flow;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.transformer.simple.SetPayloadMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clarkjohn.mule.healthchecks.HealthCheckBuilder;
import com.codahale.metrics.health.HealthCheck;

/**
 * Build individual {@link HttpHealthCheck} based on all registered {@link DefaultHttpRequester}
 * objects within the MuleContext
 *
 * @author john@clarkjohn.com
 */
@Named
public class HttpHealthCheckBuilder implements HealthCheckBuilder, MuleContextAware {

    private static Logger LOG = LoggerFactory.getLogger(HttpHealthCheckBuilder.class);

    private static final String MULE_HEALTHCHECK_TEMPLATE_FLOW_NAME = "healthcheckTemplateFlow";

    private MuleContext muleContext;

    @Override
    public Map<String, ? extends HealthCheck> getHealthChecks() {

        Map<String, HttpHealthCheck> healthCheckMap = new LinkedHashMap<>();
        for (Map.Entry<String, DefaultHttpRequester> entry : muleContext.getRegistry().lookupByType(DefaultHttpRequester.class)
                .entrySet()) {

            String flowName = "healthcheck_" + entry.getKey();

            try {
                createHealthCheckFlow(flowName, entry.getValue());
                HttpHealthCheck httpHealthCheck = new HttpHealthCheck(flowName, muleContext, toUrl(entry.getValue()));
                healthCheckMap.put(entry.getKey(), httpHealthCheck);

                LOG.info("Initialized healthcheck={}", httpHealthCheck);
            } catch (Exception e) {
                LOG.error("Unable to initialize {} for flowName={}, defaultHttpRequester={}", HttpHealthCheck.class.getSimpleName(),
                        flowName, entry.getKey(), e);
            }

        }

        return healthCheckMap;
    }

    private void createHealthCheckFlow(String flowName, DefaultHttpRequester defaultHttpRequester) throws RegistrationException {

        Flow templateFlow = muleContext.getRegistry().get(MULE_HEALTHCHECK_TEMPLATE_FLOW_NAME);

        Flow newHealthCheckFlow = new Flow(flowName, muleContext);
        newHealthCheckFlow.setInitialState(templateFlow.getInitialState());
        newHealthCheckFlow.setExceptionListener(templateFlow.getExceptionListener());
        newHealthCheckFlow.setMessageInfoMapping(templateFlow.getMessageInfoMapping());
        newHealthCheckFlow.setMessageSource(templateFlow.getMessageSource());
        newHealthCheckFlow.setProcessingStrategy(templateFlow.getProcessingStrategy());
        newHealthCheckFlow.setMessageProcessors(templateFlow.getMessageProcessors());

        List<MessageProcessor> newMessageProcessorList = new LinkedList<>();
        templateFlow.getMessageProcessors().forEach((k) -> newMessageProcessorList.add(k));

        newMessageProcessorList.add(defaultHttpRequester);
        newMessageProcessorList.add(new SetPayloadMessageProcessor());

        newHealthCheckFlow.setMessageProcessors(newMessageProcessorList);

        muleContext.getRegistry().registerObject(flowName, newHealthCheckFlow);
    }

    private String toUrl(DefaultHttpRequester defaultHttpRequester) throws MalformedURLException {

        DefaultHttpRequesterConfig config = defaultHttpRequester.getConfig();

        String host = (defaultHttpRequester.getHost() == null) ? config.getHost() : defaultHttpRequester.getHost();
        String port = (defaultHttpRequester.getPort() == null) ? config.getPort() : defaultHttpRequester.getPort();
        String basePath = config.getBasePath();
        String path = defaultHttpRequester.getPath();
        boolean isHttps = (config.getTlsContext() == null) ? false : true;

        return ((isHttps) ? "https://" : "https//") + host + ":" + port + basePath + path;
    }

    @Override
    public void setMuleContext(MuleContext muleContext) {
        this.muleContext = muleContext;
    }
}
