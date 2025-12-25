package com.proyecto.StoreCollection.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.proyecto.StoreCollection.dto.response.BoletaResponse;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;

    public byte[] generarFacturaPdf(BoletaResponse boleta) throws Exception {
        Context context = new Context();
        context.setVariable("boleta", boleta);

        String html = templateEngine.process("pdf/factura", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);

        return outputStream.toByteArray();
    }
}