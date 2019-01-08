package com.clarkjohn.mule.healthchecks.health.openport;

import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.clarkjohn.mule.healthchecks.MuleHealthCheck;
import com.codahale.metrics.health.HealthCheck;

/**
 * Healthcheck using java.net.Socket library to check if port is open on a list of hosts or IPs
 *
 * @author john@clarkjohn.com
 */
public class OpenPortHealthCheck extends MuleHealthCheck {

    private static Logger LOG = LoggerFactory.getLogger(OpenPortHealthCheck.class);

    private List<String> hostList;
    private int port;

    public OpenPortHealthCheck(List<String> hostList, int port) {
        Assert.notNull(hostList);
        Assert.notNull(port);

        this.hostList = hostList;
        this.port = port;
    }

    @Override
    public Result check() throws Exception {

        List<String> openendPortHostList = new LinkedList<>();
        Map<String, String> errorPortsToErrorMessageMap = new LinkedHashMap<>();

        for (String host : hostList) {
            SocketResult socketResult = serverListening(host, port);
            if (socketResult.isSuccess()) {
                openendPortHostList.add(host);
            } else {
                errorPortsToErrorMessageMap.put(host, socketResult.getMessage());
            }
        }

        if (errorPortsToErrorMessageMap.size() == 0) {
            return HealthCheck.Result.healthy("Connection to port=" + port + " is open on host(s)=" + openendPortHostList);
        } else {

            StringBuilder sb = new StringBuilder();
            sb.append("Connection to port=" + port + " is not opened due to " + errorPortsToErrorMessageMap);
            if (openendPortHostList.size() > 0) {
                sb.append("; Connection to port=" + port + " is open on host(s)=" + openendPortHostList);
            }

            return HealthCheck.Result.unhealthy("" + sb);
        }

    }

    private interface SocketResult {
        boolean isSuccess();

        String getMessage();
    }

    public SocketResult serverListening(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            return new SocketResult() {

                @Override
                public boolean isSuccess() {
                    return true;
                }

                @Override
                public String getMessage() {
                    return "";
                }

            };
        } catch (final Exception e) {
            return new SocketResult() {

                @Override
                public boolean isSuccess() {
                    return false;
                }

                @Override
                public String getMessage() {
                    return "" + e;
                }

            };
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    LOG.warn("Unable to close connection", e);
                }
            }
        }
    }

    @Override
    public boolean isRunsOnceADay() {
        return false;
    }

    @Override
    public String toString() {
        return "PortHealthCheck [hostList=" + hostList + ", port=" + port + "]";
    }

}
