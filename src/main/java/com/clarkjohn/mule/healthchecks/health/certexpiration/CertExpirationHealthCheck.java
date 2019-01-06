package com.clarkjohn.mule.healthchecks.health.certexpiration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.codahale.metrics.health.HealthCheck;

/**
 * Healthcheck to check if a certificate is expired or about to expire
 *
 * @author john@clarkjohn.com
 */
public class CertExpirationHealthCheck extends HealthCheck {

    private static Logger LOG = LoggerFactory.getLogger(CertExpirationHealthCheck.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.US).withZone(ZoneId.systemDefault());

    private String keystorePath;
    private String keystorePassword;
    private int numberOfdaysToWarnBeforeExpireDate;

    /**
     * Status of Certificate expiration date
     */
    private enum CertStatus {

        HEALTHY, //
        ABOUT_TO_EXPIRE, //
        EXPIRED;

        public static CertStatus getCertificateStatus(long daysUntilCertificateExpires, int numberOfdaysToWarnBeforeExpireDate) {
            if (daysUntilCertificateExpires < 0) {
                return EXPIRED;
            } else if (daysUntilCertificateExpires < numberOfdaysToWarnBeforeExpireDate) {
                return ABOUT_TO_EXPIRE;
            } else {
                return HEALTHY;
            }
        }
    };

    public CertExpirationHealthCheck(String keystorePath, String keystorePassword, int numberOfdaysToWarnBeforeExpireDate) {
        Assert.notNull(keystorePath);
        Assert.notNull(keystorePassword);

        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.numberOfdaysToWarnBeforeExpireDate = numberOfdaysToWarnBeforeExpireDate;
    }

    @Override
    public Result check() throws Exception {

        try {

            Map<CertStatus, Map<String, Long>> certStatusToCertAliasWithDaysToCertExpiresMap = checkCertificates();
            String healthMessage = getHealthMessage(certStatusToCertAliasWithDaysToCertExpiresMap);

            if (certStatusToCertAliasWithDaysToCertExpiresMap.get(CertStatus.EXPIRED).size() > 0
                    || certStatusToCertAliasWithDaysToCertExpiresMap.get(CertStatus.ABOUT_TO_EXPIRE).size() > 0) {

                return HealthCheck.Result.unhealthy(healthMessage);
            } else {
                return HealthCheck.Result.healthy(healthMessage);
            }

        } catch (Exception e) {
            return HealthCheck.Result.unhealthy("Exception trying the check certificate expiration date.  Exception=" + e.getMessage());
        }

    }

    private String getHealthMessage(Map<CertStatus, Map<String, Long>> certStatusToCertAliasWithDaysToCertExpiresMap) {

        StringBuilder sb = new StringBuilder();
        for (Entry<CertStatus, Map<String, Long>> certStatusEntry : certStatusToCertAliasWithDaysToCertExpiresMap.entrySet()) {

            sb.append("Certificates with status=" + certStatusEntry.getKey() + " and alias(es) {");
            for (Entry<String, Long> CertAliasWithDaysToCertExpiresEntry : certStatusEntry.getValue().entrySet()) {
                sb.append(" " + CertAliasWithDaysToCertExpiresEntry.getKey() + " expires in "
                        + CertAliasWithDaysToCertExpiresEntry.getValue() + " days.");
            }
            sb.append("};  ");
        }

        return sb.toString();
    }

    private Map<CertStatus, Map<String, Long>> checkCertificates()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {

        Instant currentInstant = ZonedDateTime.now().toInstant();

        Map<CertStatus, Map<String, Long>> certStatusToCertAliasWithDaysToCertExpiresMap = new LinkedHashMap<>();
        // init map with each CertStatus
        for (CertStatus certStatus : CertStatus.values()) {
            certStatusToCertAliasWithDaysToCertExpiresMap.put(certStatus, new LinkedHashMap<>());
        }

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());

        for (String certAlias : Collections.list(keystore.aliases())) {

            Instant certExpirationInstant = (((X509Certificate) keystore.getCertificate(certAlias)).getNotAfter()).toInstant();
            long daysToCertExpires = ChronoUnit.DAYS.between(currentInstant, certExpirationInstant);
            CertStatus certStatus = CertStatus.getCertificateStatus(daysToCertExpires, numberOfdaysToWarnBeforeExpireDate);

            certStatusToCertAliasWithDaysToCertExpiresMap.get(certStatus).put(certAlias, daysToCertExpires);

            LOG.info(
                    "Certificate details from keystore={}, alias={}, certificate expire status={}, certificate expire date={}, days to expire={}, certificate details={}",
                    keystorePath, certAlias, certStatus, DATE_TIME_FORMATTER.format(certExpirationInstant), daysToCertExpires,
                    keystore.getCertificate(certAlias));

        }

        return certStatusToCertAliasWithDaysToCertExpiresMap;
    }

    @Override
    public String toString() {
        return "CertExpirationHealthCheck [keystorePath=" + keystorePath + ", numberOfdaysToWarnBeforeExpireDate="
                + numberOfdaysToWarnBeforeExpireDate + "]";
    }

}
