package com.clarkjohn.mule.healthchecks.health.http;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.construct.Flow;
import org.mule.session.DefaultMuleSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.codahale.metrics.health.HealthCheck;

/**
 * Mule http.status 2xx healthcheck
 *
 * @author john@clarkjohn.com
 */
public class HttpHealthCheck extends HealthCheck {

    private static Logger LOG = LoggerFactory.getLogger(HttpHealthCheck.class);

    private String flowName;
    private MuleContext muleContext;
    private String url;

    public HttpHealthCheck(String flowName, MuleContext muleContext, String url) {
        Assert.notNull(flowName);
        Assert.notNull(muleContext);
        Assert.notNull(url);

        this.flowName = flowName;
        this.muleContext = muleContext;
        this.url = url;
    }

    @Override
    protected Result check() {

        try {
            Flow flow = muleContext.getRegistry().get(flowName);
            FlowConstruct flowConstruct = muleContext.getRegistry().lookupFlowConstruct(flowName);

            MuleMessage originalMuleMessage = new DefaultMuleMessage("", muleContext);
            MuleEvent inputEvent = new DefaultMuleEvent(originalMuleMessage, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct,
                    new DefaultMuleSession());
            MuleMessage muleMessage = flow.process(inputEvent).getMessage();

            String httpStatus = "" + muleMessage.getInboundProperty("http.status");

            if (is2xxResponseCode(httpStatus)) {
                return HealthCheck.Result.healthy("Successfull 2xx connection to url=" + url);
            } else {
                StringBuilder sb = new StringBuilder("http.status=" + httpStatus);
                Exception exception = muleMessage.getInvocationProperty("healthExceptionMessage");
                if (exception != null) {
                    sb.append(", exceptionMessage=" + exception.getMessage());
                }
                return HealthCheck.Result.unhealthy("Unsuccessfull connection to url=" + url + ", " + sb);
            }

        } catch (Exception e) {
            String errorMessage = "Unable to run healthcheck=" + flowName + ", url=" + url + ", errorMessage=" + e.getMessage();
            LOG.error(errorMessage, e);
            return HealthCheck.Result.unhealthy(errorMessage);
        }
    }

    private boolean is2xxResponseCode(String httpStatus) {
        return (httpStatus != null && httpStatus.length() > 0 && httpStatus.charAt(0) == '2') ? true : false;
    }

    @Override
    public String toString() {
        return "HttpHealthCheck [flowName=" + flowName + ", url=" + url + "]";
    }

}
