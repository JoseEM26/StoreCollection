package com.proyecto.StoreCollection.controller;

import com.proyecto.StoreCollection.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class TestUploadController {

    private final CloudinaryService cloudinaryService;

    public TestUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/test-upload")
    public ResponseEntity<Map> testUpload(@RequestParam("file") MultipartFile file) {
        try {
            Map result = cloudinaryService.upload(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}