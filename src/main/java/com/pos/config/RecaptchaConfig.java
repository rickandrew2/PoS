package com.pos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecaptchaConfig {
    @Value("${recaptcha.secret.key}")
    private String secretKey;

    @Value("${recaptcha.site.key}")
    private String siteKey;

    public String getSecretKey() {
        return secretKey;
    }

    public String getSiteKey() {
        return siteKey;
    }
} 