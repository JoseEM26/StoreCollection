package com.proyecto.StoreCollection.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(15));
        factory.setMaxRequestSize(DataSize.ofMegabytes(100));
        // No hay setMaxPartCount aquí en la mayoría de versiones 3.x
        return factory.createMultipartConfig();
    }
}