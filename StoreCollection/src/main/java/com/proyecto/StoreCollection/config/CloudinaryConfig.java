package com.proyecto.StoreCollection.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dqznlmig0");
        config.put("api_key", "947154626432549");
        config.put("api_secret", "FzBvMxi5-_P1_o6wzDfhFkGACtY");
        config.put("secure", "true");

        return new Cloudinary(config);
    }
}