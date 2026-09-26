package com.gms.filesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyRequest {
    private String apiKey;
    private String partnerName;
    private String permissions;
}
