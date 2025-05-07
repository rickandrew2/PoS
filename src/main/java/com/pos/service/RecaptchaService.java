package com.pos.service;

import com.pos.config.RecaptchaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class RecaptchaService {
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Logger logger = LoggerFactory.getLogger(RecaptchaService.class);

    @Autowired
    private RecaptchaConfig recaptchaConfig;

    @Autowired
    private RestTemplate restTemplate;

    public boolean verifyRecaptcha(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.trim().isEmpty()) {
            logger.warn("Empty reCAPTCHA response received");
            return false;
        }

        try {
            logger.debug("Verifying reCAPTCHA response: {}", recaptchaResponse);
            
            Map<String, String> request = new HashMap<>();
            request.put("secret", recaptchaConfig.getSecretKey());
            request.put("response", recaptchaResponse);
        
            logger.debug("Sending reCAPTCHA verification request to Google");
        
            Map<String, Object> response = restTemplate.postForObject(
                RECAPTCHA_VERIFY_URL,
                request,
                Map.class
            );
        
            if (response == null) {
                logger.error("Null response received from Google reCAPTCHA verification");
                return false;
            }

            logger.debug("Google reCAPTCHA response: {}", response);
            
            boolean success = Boolean.TRUE.equals(response.get("success"));
            if (!success) {
                logger.warn("reCAPTCHA verification failed. Error codes: {}", response.get("error-codes"));
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error verifying reCAPTCHA: {}", e.getMessage(), e);
            return false;
        }
    }
} 