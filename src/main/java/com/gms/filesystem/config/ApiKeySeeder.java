package com.gms.filesystem.config;

import com.gms.filesystem.entity.ApiKey;
import com.gms.filesystem.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Ensures gateway-service's own internal API key always exists and is
 * active/admin in this service's api_keys table, so enabling ApiKeyAuthFilter
 * never locks gateway-service out of its own backend on startup.
 */
@Component
public class ApiKeySeeder implements ApplicationRunner {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Value("${internal.gateway-api-key}")
    private String gatewayApiKey;

    @Override
    public void run(ApplicationArguments args) {
        apiKeyRepository.findByApiKey(gatewayApiKey).ifPresentOrElse(
                existing -> {
                    if (!existing.isActive() || !existing.isAdmin()) {
                        existing.setActive(true);
                        existing.setAdmin(true);
                        apiKeyRepository.save(existing);
                    }
                },
                () -> {
                    ApiKey key = new ApiKey();
                    key.setApiKey(gatewayApiKey);
                    key.setPartnerName("gateway-service-internal");
                    key.setActive(true);
                    key.setAdmin(true);
                    key.setCreatedAt(LocalDateTime.now());
                    apiKeyRepository.save(key);
                }
        );
    }
}
