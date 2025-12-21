package com.proyecto.StoreCollection.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Subir imagen (sin opciones extras)
    public Map upload(MultipartFile file) throws IOException {
        File tempFile = convert(file);
        Map result = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap());
        tempFile.delete();
        return result;
    }

    // Subir con opciones (ej: carpeta, public_id, tags, etc.)
    public Map upload(MultipartFile file, Map<String, Object> options) throws IOException {
        File tempFile = convert(file);
        Map result = cloudinary.uploader().upload(tempFile, options);
        tempFile.delete();
        return result;
    }

    // Eliminar por public_id
    public Map delete(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // Generar URL transformada usando la API de versión 2.x
    public String getTransformedUrl(String publicId, Transformation transformation) {
        return cloudinary.url()
                .transformation(transformation)
                .secure(true) // Siempre HTTPS en producción
                .generate(publicId);
    }

    // Ejemplo: URL con redimensión y optimizaciones automáticas
    public String getResizedUrl(String publicId, int width, int height) {
        Transformation transformation = new Transformation()
                .width(width)
                .height(height)
                .crop("fill")           // String para el modo de crop
                .quality("auto")        // String para calidad automática
                .fetchFormat("auto");   // String para formato automático (webp, avif, etc.)

        return cloudinary.url()
                .transformation(transformation)
                .secure(true)
                .generate(publicId);
    }

    // Conversor MultipartFile → File temporal
    private File convert(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }
}