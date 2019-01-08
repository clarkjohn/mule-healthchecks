mule-healthchecks
===================

Healthchecks to be used with the Mule ESB.

## Types of included mule-healthchecks:

### 1. http

Http healthchecks check for successful 2xx http status codes on all downstream service endpoints.   

Healthchecks are generated at startup based on all Mule defined 
[http-connectors](https://docs.mulesoft.com/connectors/http/http-connector) within Mule flows

### 2. openport

Open port healthchecks check for socket connections using the java.net.Socket library.  Open Port healthchecks are useful to check for network connectivity for known failover virtual server IPs on a load balancer such as a F5 LTM.

Open port connections are created in two ways:
1. Properties file
2. Generated at startup based on all Mule defined [http-connectors](https://docs.mulesoft.com/connectors/http/http-connector) within Mule flows.  Open port healthchecks will use the hostname/IP defined in the http-connector

### 3. certificate expiration

Certificate Expiration healthchecks check if a certificate has expired or is about to expire.   

Healthchecks are generated at startup based on keystores/truststores using all Mule defined DefaultTlsContextFactorys 
