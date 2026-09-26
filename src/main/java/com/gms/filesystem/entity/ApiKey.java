package com.gms.filesystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "api_key", nullable = false, unique = true, length = 128)
    private String apiKey;

    @NotBlank
    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(nullable = false)
    private boolean active = true;

    // True only for the single key gateway-service itself uses - lets the
    // /api/api-keys management endpoints distinguish "gateway issuing a new
    // partner key" from "a partner key being used to try to mint/revoke keys".
    // Admin keys also bypass the per-API permission check below entirely.
    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    // Comma-separated permission codes (UPLOAD, DOWNLOAD, READ, DELETE)
    // granting this key access to that action. Empty = no access beyond a
    // valid key. Checked by ApiKeyAuthFilter.
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions = "";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isBlank()) {
            return false;
        }
        for (String p : permissions.split(",")) {
            if (p.trim().equalsIgnoreCase(permission)) {
                return true;
            }
        }
        return false;
    }
}
