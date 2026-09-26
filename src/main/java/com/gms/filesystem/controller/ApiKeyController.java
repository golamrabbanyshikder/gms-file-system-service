package com.gms.filesystem.controller;

import com.gms.filesystem.dto.ApiKeyRequest;
import com.gms.filesystem.entity.ApiKey;
import com.gms.filesystem.repository.ApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Manages this service's own api_keys table. Only callable with an admin
 * key (gateway-service's seeded internal key) - a regular partner key must
 * not be able to mint or revoke keys for itself or anyone else.
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @PostMapping
    public ResponseEntity<?> createOrReactivate(@RequestBody ApiKeyRequest request, HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return forbidden();
        }
        if (request.getApiKey() == null || request.getApiKey().isBlank()
                || request.getPartnerName() == null || request.getPartnerName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "apiKey and partnerName are required"));
        }

        ApiKey apiKey = apiKeyRepository.findByApiKey(request.getApiKey()).orElseGet(ApiKey::new);
        apiKey.setApiKey(request.getApiKey());
        apiKey.setPartnerName(request.getPartnerName());
        apiKey.setActive(true);
        apiKey.setPermissions(request.getPermissions() != null ? request.getPermissions() : "");
        if (apiKey.getCreatedAt() == null) {
            apiKey.setCreatedAt(LocalDateTime.now());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyRepository.save(apiKey));
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return forbidden();
        }
        return ResponseEntity.ok(apiKeyRepository.findAll());
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody ApiKeyRequest request, HttpServletRequest httpRequest) {
        if (!isAdmin(httpRequest)) {
            return forbidden();
        }
        return apiKeyRepository.findByApiKey(request.getApiKey())
                .map(key -> {
                    key.setActive(false);
                    return ResponseEntity.ok(apiKeyRepository.save(key));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAdmin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute("apiKeyIsAdmin"));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin API key required"));
    }
}
