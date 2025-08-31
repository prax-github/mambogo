package com.mambogo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Configuration properties for request validation at the gateway level.
 */
@Component
@ConfigurationProperties(prefix = "gateway.validation")
public class RequestValidationProperties {

    private long maxRequestSize = 1024 * 1024; // 1MB default
    private long maxHeaderSize = 8192; // 8KB default
    private int maxHeadersCount = 50;
    private int maxQueryParams = 20;
    private int maxPathLength = 2048;
    private boolean enableSecurityValidation = true;
    private boolean enableContentTypeValidation = true;
    private boolean enableSizeValidation = true;
    
    private Set<String> allowedContentTypes = Set.of(
        "application/json",
        "application/x-www-form-urlencoded",
        "multipart/form-data",
        "text/plain"
    );
    
    private Set<String> trustedIpRanges = Set.of(
        "127.0.0.1",
        "::1",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16"
    );

    // Getters and Setters
    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public long getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(long maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxHeadersCount() {
        return maxHeadersCount;
    }

    public void setMaxHeadersCount(int maxHeadersCount) {
        this.maxHeadersCount = maxHeadersCount;
    }

    public int getMaxQueryParams() {
        return maxQueryParams;
    }

    public void setMaxQueryParams(int maxQueryParams) {
        this.maxQueryParams = maxQueryParams;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public boolean isEnableSecurityValidation() {
        return enableSecurityValidation;
    }

    public void setEnableSecurityValidation(boolean enableSecurityValidation) {
        this.enableSecurityValidation = enableSecurityValidation;
    }

    public boolean isEnableContentTypeValidation() {
        return enableContentTypeValidation;
    }

    public void setEnableContentTypeValidation(boolean enableContentTypeValidation) {
        this.enableContentTypeValidation = enableContentTypeValidation;
    }

    public boolean isEnableSizeValidation() {
        return enableSizeValidation;
    }

    public void setEnableSizeValidation(boolean enableSizeValidation) {
        this.enableSizeValidation = enableSizeValidation;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(Set<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public Set<String> getTrustedIpRanges() {
        return trustedIpRanges;
    }

    public void setTrustedIpRanges(Set<String> trustedIpRanges) {
        this.trustedIpRanges = trustedIpRanges;
    }
}
