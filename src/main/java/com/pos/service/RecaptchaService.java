package com.pos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret.key}")
    private String secretKey;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyRecaptcha(String recaptchaResponse) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", secretKey);
        requestMap.add("response", recaptchaResponse);

        try {
            RecaptchaResponse response = restTemplate.postForObject(
                RECAPTCHA_VERIFY_URL,
                requestMap,
                RecaptchaResponse.class
            );
            return response != null && response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    private static class RecaptchaResponse {
        private boolean success;
        private String challenge_ts;
        private String hostname;
        private double score;
        private String action;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
} 