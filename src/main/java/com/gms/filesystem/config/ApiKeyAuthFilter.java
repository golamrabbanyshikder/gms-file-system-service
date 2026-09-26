package com.gms.filesystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gms.filesystem.entity.ApiKey;
import com.gms.filesystem.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Requires a valid, active API key (header X-API-Key) on every request to
 * this service, AND - for non-admin (partner) keys - that the key has been
 * granted the specific permission for the action being called (UPLOAD,
 * DOWNLOAD, READ, DELETE). A partner issued only DOWNLOAD access must not be
 * able to upload or delete files. gateway-service's own internal key
 * (seeded by ApiKeySeeder, admin=true) bypasses this check entirely.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-API-Key";

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String headerKey = request.getHeader(HEADER_NAME);
        if (headerKey == null || headerKey.isBlank()) {
            reject(request, response, "Missing API key. Include it in the " + HEADER_NAME + " header.");
            return;
        }

        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKeyAndActiveTrue(headerKey);
        if (apiKeyOpt.isEmpty()) {
            reject(request, response, "Invalid or revoked API key.");
            return;
        }
        ApiKey apiKey = apiKeyOpt.get();

        if (!apiKey.isAdmin()) {
            String requiredPermission = resolveRequiredPermission(request);
            if (requiredPermission != null && !apiKey.hasPermission(requiredPermission)) {
                forbid(request, response, "API key for '" + apiKey.getPartnerName()
                        + "' does not have the '" + requiredPermission + "' permission.");
                return;
            }
        }

        request.setAttribute("apiKeyPartner", apiKey.getPartnerName());
        request.setAttribute("apiKeyIsAdmin", apiKey.isAdmin());
        filterChain.doFilter(request, response);
    }

    private String resolveRequiredPermission(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.startsWith("/api/api-keys")) {
            return null;
        }
        if (path.equals("/api/files/upload")) {
            return "UPLOAD";
        }
        if (path.startsWith("/api/files/download/")) {
            return "DOWNLOAD";
        }
        if (path.startsWith("/api/files/delete/")) {
            return "DELETE";
        }
        if (path.startsWith("/api/files")) {
            return "READ";
        }
        return "UNKNOWN";
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", message, request.getRequestURI());
    }

    private void forbid(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", message, request.getRequestURI());
    }

    private void writeError(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
