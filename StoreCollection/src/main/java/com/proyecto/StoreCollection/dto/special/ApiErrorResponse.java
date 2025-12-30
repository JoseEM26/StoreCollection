package com.proyecto.StoreCollection.dto.special;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;

    private String error;

    private String message;

    private String details;

    private String path;

    // Constructor personalizado para el handler (sin timestamp)
    public ApiErrorResponse(int status, String error, String message, String details, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = LocalDateTime.now(); // Asegura que siempre tenga valor
    }
}