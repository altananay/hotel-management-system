package com.hotel.pricing.internal.application;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hotel.pricing")
public class PricingProperties {

    private final Tcmb tcmb = new Tcmb();
    private final Defaults defaults = new Defaults();
    private Duration quoteTtl = Duration.ofMinutes(15);
    private int maxStayNights = 30;

    public Tcmb getTcmb() {
        return tcmb;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public Duration getQuoteTtl() {
        return quoteTtl;
    }

    public void setQuoteTtl(Duration quoteTtl) {
        this.quoteTtl = quoteTtl;
    }

    public int getMaxStayNights() {
        return maxStayNights;
    }

    public void setMaxStayNights(int maxStayNights) {
        if (maxStayNights < 1) {
            throw new IllegalArgumentException("hotel.pricing.max-stay-nights must be at least 1");
        }
        this.maxStayNights = maxStayNights;
    }

    public static class Tcmb {

        private boolean enabled = true;
        private String url = "https://www.tcmb.gov.tr/kurlar/today.xml";
        private String cron = "0 30 15 * * MON-FRI";
        private String zone = "Europe/Istanbul";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Defaults {

        private BigDecimal bufferPercent = new BigDecimal("2.00");
        private BigDecimal roundingStep = new BigDecimal("5.00");

        public BigDecimal getBufferPercent() {
            return bufferPercent;
        }

        public void setBufferPercent(BigDecimal bufferPercent) {
            this.bufferPercent = bufferPercent;
        }

        public BigDecimal getRoundingStep() {
            return roundingStep;
        }

        public void setRoundingStep(BigDecimal roundingStep) {
            this.roundingStep = roundingStep;
        }
    }
}
