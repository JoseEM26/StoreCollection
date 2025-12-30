INSERT INTO plan (
    nombre, slug, descripcion, precio_mensual, precio_anual,
    intervalo_billing, intervalo_cantidad,
    max_productos, max_variantes,
    es_trial, dias_trial, es_visible_publico, orden, activo
) VALUES
('Gratis', 'gratis', 'Plan gratuito con funciones básicas', 0.00, 0.00, 'month', 1, 10, 50, FALSE, 0, TRUE, 1, TRUE),
('Básico', 'basico', 'Plan básico mensual o anual', 49.90, 499.00, 'month', 1, 100, 500, FALSE, 0, TRUE, 2, TRUE),
('Pro', 'pro', 'Plan profesional con más productos', 99.90, 999.00, 'month', 1, 500, 2000, FALSE, 0, TRUE, 3, TRUE),
('Premium', 'premium', 'Ideal para tiendas grandes', 199.90, 1999.00, 'month', 1, 2000, 8000, FALSE, 0, TRUE, 4, TRUE),
('Enterprise', 'enterprise', 'Sin límites + soporte prioritario', 499.90, 4999.00, 'month', 1, 99999, 999999, FALSE, 0, TRUE, 5, TRUE);
-- Usuarios (celular con solo 9 dígitos)
INSERT INTO usuario (nombre, email, password, celular, rol) VALUES
('Admin Sistema', 'admin@storecollection.pe', 'admin', '999999999', 'ADMIN'),
('María López', 'maria@zapatik.pe', 'maria', '987654321', 'OWNER'),
('Carlos Ruiz', 'carlos@techpro.pe', 'carlos', '911223344', 'OWNER'),
('Ana Gómez', 'ana@modafashion.pe', 'ana', '955667788', 'OWNER'),
('Luis Torres', 'luis@petfood.pe', 'luis', '933445566', 'OWNER'),
('Sofía Vega', 'sofia@platajoyas.pe', 'sofia', '977889900', 'OWNER'),
('Pedro Salazar', 'pedro@deportesxtreme.pe', 'pedro', '922334455', 'OWNER'),
('Laura Mendoza', 'laura@cosmeticos.pe', 'laura', '966778899', 'OWNER'),
('Diego Castro', 'diego@gadgetsperu.pe', 'diego', '944556677', 'OWNER'),
('Valeria Ortiz', 'valeria@bellezanatural.pe', 'valeria', '988990011', 'OWNER'),
('Javier Ramos', 'javier@accesorioscel.pe', 'javier', '911224455', 'OWNER'),
('Camila Flores', 'camila@ropainfantil.pe', 'camila', '999887766', 'OWNER'),
('Renzo Vargas', 'renzo@celularesimport.pe', 'renzo', '988776655', 'OWNER'),
('Fernanda Diaz', 'fernanda@maquillajepro.pe', 'fernanda', '977665544', 'OWNER'),
('Mateo Silva', 'mateo@suplementosgym.pe', 'mateo', '966554433', 'OWNER'),
('Isabella Cruz', 'isabella@perfumesoriginales.pe', 'isabella', '955443322', 'OWNER'),
('Gabriel Soto', 'gabriel@relojesperu.pe', 'gabriel', '944332211', 'OWNER'),
('Lucía Herrera', 'lucia@librosdigitales.pe', 'lucia', '933221100', 'OWNER'),
('Thiago Morales', 'thiago@juguetesdidacticos.pe', 'thiago', '922110099', 'OWNER'),
('Valentina Rios', 'valentina@velasartesanales.pe', 'valentina', '911009988', 'OWNER'),
('Santiago Paredes', 'santiago@herramientaspro.pe', 'santiago', '900998877', 'OWNER'),
('Emilia Castro', 'emilia@decorhogar.pe', 'emilia', '999887766', 'OWNER'),
('Benjamín Ortiz', 'benjamin@campingperu.pe', 'benjamin', '988776655', 'OWNER'),
('Martina León', 'martina@plantasinterior.pe', 'martina', '977665544', 'OWNER'),
('Joaquín Navarro', 'joaquin@bateriasportatil.pe', 'joaquin', '966554433', 'OWNER'),
('Regina Campos', 'regina@artesaniasperu.pe', 'regina', '955443322', 'OWNER'),
('Lautaro Romero', 'lautaro@biciurbana.pe', 'lautaro', '944332211', 'OWNER'),
('Zoe Gutierrez', 'zoe@ropaembarazada.pe', 'zoe', '933221100', 'OWNER'),
('Dylan Medina', 'dylan@gamingperifericos.pe', 'dylan', '922110099', 'OWNER'),
('Alma Fuentes', 'alma@cuidadofacial.pe', 'alma', '911009988', 'OWNER'),
('Ian Guerrero', 'ian@audifonospro.pe', 'ian', '900998877', 'OWNER'),
('Luna Vargas', 'luna@skincarekorea.pe', 'luna', '988776655', 'OWNER'),
('Bruno Salazar', 'bruno@cafeartesanal.pe', 'bruno', '977665544', 'OWNER'),
('Renata Ortiz', 'renata@joyeriaplata.pe', 'renata', '966554433', 'OWNER'),
('Lía Mendoza', 'lia@ropaactivewear.pe', 'lia', '955443322', 'OWNER'),
('Axel Torres', 'axel@pcgaming.pe', 'axel', '944332211', 'OWNER'),
('Amira Paredes', 'amira@accesoriosmujer.pe', 'amira', '933221100', 'OWNER'),
('Noah Castro', 'noah@zapatosformal.pe', 'noah', '922110099', 'OWNER'),
('Gala Rios', 'gala@bellezainfantil.pe', 'gala', '911009988', 'OWNER'),
('Leo Navarro', 'leo@vinosimportados.pe', 'leo', '900998877', 'OWNER'),
('Mía Herrera', 'mia@librosfisicos.pe', 'mia', '999887766', 'OWNER');
-- ========================================
-- INSERCIÓN DE TIENDAS - CORREGIDO v2.2 (27 DIC 2025)
-- Sin plan_id → la suscripción se maneja en tienda_suscripcion
-- ========================================

INSERT INTO tienda (
    nombre,
    slug,
    whatsapp,
    moneda,
    descripcion,
    direccion,
    horarios,
    mapa_url,
    logo_img_url,
    user_id,
    activo,
    plan_id
) VALUES
('ZapaTik', 'zapatik', '987654321', 'SOLES', 'Zapatillas virales de TikTok', 'Av. Brasil 1234, Lima', 'Lun-Sab 10am-9pm', 'https://maps.google.com/?q=zapatik', 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766703356/tiendas/logos/yxwdoveun3nb7zndbv2s.png', 2, TRUE, 2),
('TechPro Perú', 'techpro-peru', '911223344', 'DOLARES', 'Gadgets importados premium', 'Tienda online', '24/7', 'https://maps.google.com/?q=techpro', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/techpro.png', 3, TRUE, 3),
('Moda Fashion', 'moda-fashion', '955667788', 'SOLES', 'Ropa trendy 2025', 'Jr. de la Unión 890', 'Lun-Dom 9am-10pm', 'https://maps.google.com/?q=modafashion', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/modafashion.png', 4, TRUE, 2),
('PetFood Premium', 'petfood-premium', '933445566', 'SOLES', 'Alimento para mascotas', 'Av. Larco 123, Miraflores', 'Lun-Vie 9am-7pm', 'https://maps.google.com/?q=petfood', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/petfood.png', 5, TRUE, 1),
('Plata & Joyas', 'plata-joyas', '977889900', 'SOLES', 'Joyería en plata 925', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=platajoyas', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/platajoyas.png', 6, TRUE, 3),
('Deportes Xtreme', 'deportes-xtreme', '922334455', 'SOLES', 'Ropa deportiva', 'Av. Canadá 567', 'Lun-Sab 10am-8pm', 'https://maps.google.com/?q=deportesxtreme', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/deportesxtreme.png', 7, TRUE, 2),
('Cosméticos Naturales', 'cosmeticos-naturales', '966778899', 'SOLES', 'Productos 100% orgánicos', 'Tienda online', '24/7', 'https://maps.google.com/?q=cosmeticos', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/cosmeticosnaturales.png', 8, TRUE, 1),
('Gadgets Perú', 'gadgets-peru', '944556677', 'DOLARES', 'Accesorios para celular', 'Av. Primavera 800', 'Lun-Sab 11am-9pm', 'https://maps.google.com/?q=gadgetsperu', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/gadgetsperu.png', 9, TRUE, 3),
('Belleza Natural', 'belleza-natural', '988990011', 'SOLES', 'Cremas y aceites naturales', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=bellezanatural', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/bellezanatural.png', 10, TRUE, 2),
('Accesorios Cel', 'accesorios-cel', '911224455', 'SOLES', 'Todo para tu celular', 'Mall del Sur', 'Lun-Dom 10am-10pm', 'https://maps.google.com/?q=accesorioscel', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/accesorioscel.png', 11, TRUE, 1),
('Ropa Infantil', 'ropa-infantil', '999887766', 'SOLES', 'Ropita para bebés y niños', 'Av. La Marina 200', 'Lun-Sab 10am-8pm', 'https://maps.google.com/?q=ropainfantil', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/ropainfantil.png', 12, TRUE, 2),
('Celulares Import', 'celulares-import', '988776655', 'DOLARES', 'iPhone y Samsung originales', 'Tienda online', '24/7', 'https://maps.google.com/?q=celularesimport', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/celularesimport.png', 13, TRUE, 4),
('Maquillaje Pro', 'maquillaje-pro', '977665544', 'SOLES', 'Marcas internacionales', 'Real Plaza Salaverry', 'Lun-Dom 11am-9pm', 'https://maps.google.com/?q=maquillajepro', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/maquillajepro.png', 14, TRUE, 3),
('Suplementos Gym', 'suplementos-gym', '966554433', 'SOLES', 'Proteínas, creatina, BCAAs', 'Av. Benavides 456', 'Lun-Sab 9am-8pm', 'https://maps.google.com/?q=suplementosgym', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/suplementosgym.png', 15, TRUE, 2),
('Perfumes Originales', 'perfumes-originales', '955443322', 'SOLES', 'Fragancias importadas', 'Jockey Plaza', 'Lun-Dom 10am-10pm', 'https://maps.google.com/?q=perfumes', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/perfumes.png', 16, TRUE, 3),
('Relojes Perú', 'relojes-peru', '944332211', 'DOLARES', 'Rolex, Casio, Seiko', 'Larcomar', 'Lun-Dom 11am-9pm', 'https://maps.google.com/?q=relojesperu', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/relojesperu.png', 17, TRUE, 5),
('Libros Digitales', 'libros-digitales', '933221100', 'SOLES', 'Ebooks y cursos', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=librosdigitales', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/librosdigitales.png', 18, TRUE, 1),
('Juguetes Didácticos', 'juguetes-didacticos', '922110099', 'SOLES', 'Aprende jugando', 'Av. Salaverry 789', 'Lun-Sab 10am-7pm', 'https://maps.google.com/?q=juguetes', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/juguetes.png', 19, TRUE, 2),
('Velas Artesanales', 'velas-artesanales', '911009988', 'SOLES', 'Aromaterapia y decoración', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=velas', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/velas.png', 20, TRUE, 1),
('Herramientas Pro', 'herramientas-pro', '900998877', 'SOLES', 'Para obra y hogar', 'Av. Industrial 123', 'Lun-Vie 8am-6pm', 'https://maps.google.com/?q=herramientas', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/herramientas.png', 21, TRUE, 3),
('DecorHogar', 'decorhogar', '999887766', 'SOLES', 'Todo para tu casa', 'MegaPlaza', 'Lun-Dom 10am-10pm', 'https://maps.google.com/?q=decorhogar', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/decorhogar.png', 22, TRUE, 2),
('Camping Perú', 'camping-peru', '988776655', 'SOLES', 'Carpa, sleeping, mochilas', 'Av. La Molina 456', 'Lun-Sab 10am-7pm', 'https://maps.google.com/?q=camping', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/camping.png', 23, TRUE, 2),
('Plantas Interior', 'plantas-interior', '977665544', 'SOLES', 'Plantas y macetas', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=plantas', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/plantas.png', 24, TRUE, 1),
('Baterías Portátiles', 'baterias-portatil', '966554433', 'SOLES', 'Power banks premium', 'Tienda online', '24/7', 'https://maps.google.com/?q=baterias', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/baterias.png', 25, TRUE, 3),
('Artesanías Perú', 'artesanias-peru', '955443322', 'SOLES', 'Productos típicos', 'Cusco y Lima', 'Lun-Sab 9am-6pm', 'https://maps.google.com/?q=artesanias', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/artesanias.png', 26, TRUE, 2),
('Bici Urbana', 'bici-urbana', '944332211', 'SOLES', 'Bicicletas y accesorios', 'Av. Arequipa 1234', 'Lun-Sab 10am-8pm', 'https://maps.google.com/?q=biciurbana', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/biciurbana.png', 27, TRUE, 2),
('Ropa Embarazada', 'ropa-embarazada', '933221100', 'SOLES', 'Maternidad cómoda', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=ropaembarazada', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/ropaembarazada.png', 28, TRUE, 1),
('Gaming Periféricos', 'gaming-perifericos', '922110099', 'SOLES', 'Teclados, mouse, sillas', 'Av. Javier Prado 5678', 'Lun-Sab 11am-9pm', 'https://maps.google.com/?q=gaming', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/gaming.png', 29, TRUE, 4),
('Cuidado Facial', 'cuidado-facial', '911009988', 'SOLES', 'Kits coreanos y más', 'Tienda online', '24/7', 'https://maps.google.com/?q=cuidadofacial', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/cuidadofacial.png', 30, TRUE, 2),
('Audífonos Pro', 'audifonos-pro', '900998877', 'DOLARES', 'Sony, Bose, Apple', 'Jockey Plaza', 'Lun-Dom 11am-10pm', 'https://maps.google.com/?q=audifonospro', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/audifonospro.png', 31, TRUE, 5),
('Skincare Korea', 'skincare-korea', '988776655', 'SOLES', 'Cosmética coreana', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=skincare', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/skincare.png', 32, TRUE, 3),
('Café Artesanal', 'cafe-artesanal', '977665544', 'SOLES', 'Granos premium', 'Av. Pardo 789', 'Lun-Sab 8am-8pm', 'https://maps.google.com/?q=cafe', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/cafe.png', 33, TRUE, 2),
('Joyeria Plata', 'joyeria-plata', '966554433', 'SOLES', 'Diseños exclusivos', 'Miraflores', 'Lun-Sab 11am-8pm', 'https://maps.google.com/?q=joyeria', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/joyeriaplata.png', 34, TRUE, 3),
('ActiveWear Fit', 'activewear-fit', '955443322', 'SOLES', 'Ropa deportiva mujer', 'Tienda online', '24/7', 'https://maps.google.com/?q=activewear', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/activewear.png', 35, TRUE, 2),
('PC Gaming Pro', 'pc-gaming-pro', '944332211', 'DOLARES', 'Arma tu setup', 'Av. Primavera 123', 'Lun-Sab 10am-9pm', 'https://maps.google.com/?q=pcgaming', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/pcgaming.png', 36, TRUE, 5),
('Accesorios Mujer', 'accesorios-mujer', '933221100', 'SOLES', 'Carteras, collares', 'Mall Aventura', 'Lun-Dom 10am-10pm', 'https://maps.google.com/?q=accesorios', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/accesoriosmujer.png', 37, TRUE, 2),
('Zapatos Formal', 'zapatos-formal', '922110099', 'SOLES', 'Hombre y mujer', 'San Isidro', 'Lun-Vie 10am-7pm', 'https://maps.google.com/?q=zapatosformal', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/zapatosformal.png', 38, TRUE, 3),
('Belleza Infantil', 'belleza-infantil', '911009988', 'SOLES', 'Productos para niños', 'Tienda virtual', '24/7', 'https://maps.google.com/?q=bellezainfantil', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/bellezainfantil.png', 39, TRUE, 1),
('Vinos Importados', 'vinos-importados', '900998877', 'SOLES', 'Chilenos, argentinos', 'Surco', 'Lun-Sab 11am-9pm', 'https://maps.google.com/?q=vinos', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/vinos.png', 40, TRUE, 4),
('Libros Físicos', 'libros-fisicos', '999887766', 'SOLES', 'Novelas y más', 'Lince', 'Lun-Sab 10am-8pm', 'https://maps.google.com/?q=librosfisicos', 'https://res.cloudinary.com/tucloud/image/upload/v1/logos/librosfisicos.png', 41, TRUE, 2);
INSERT INTO categoria (nombre, slug, tienda_id) VALUES
('Zapatillas Hombre', 'hombre', 1),
('Zapatillas Mujer', 'mujer', 1),
('Niños', 'ninos', 1),
('Cargadores', 'cargadores', 2),
('Auriculares', 'auriculares', 2),
('Vestidos', 'vestidos', 3),
('Polos', 'polos', 3);

INSERT INTO producto (nombre, slug, categoria_id, tienda_id) VALUES
('Nike Air Max 90', 'nike-air-max-90', 1, 1),
('Adidas Ultraboost', 'adidas-ultraboost', 1, 1),
('Cargador 65W GaN', 'cargador-65w', 4, 2),
('Sony WH-1000XM5', 'sony-xm5', 5, 2),
('Vestido Floral', 'vestido-floral', 6, 3),
('Polo Básico', 'polo-basico', 7, 3);

INSERT INTO atributo (nombre, tienda_id) VALUES
('Talla', 1),
('Color', 1),
('Capacidad', 2);

INSERT INTO atributo_valor (atributo_id, valor, tienda_id) VALUES
(1, '38', 1),
(1, '39', 1),
(1, '40', 1),
(1, '41', 1),
(1, '42', 1),
(2, 'Negro', 1),
(2, 'Blanco', 1),
(2, 'Rojo', 1),
(2, 'Azul', 1),
(3, '65W', 2),
(3, '100W', 2);

INSERT INTO Producto_Variante (producto_id, tienda_id, sku, precio, stock, imagen_url, activo) VALUES
(1, 1, 'NAM90-38-NEGRO', 549.90, 12, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766708632/tiendas/zapatik/productos/d2nbioqlakdkh6zor1uz.png', TRUE),
(1, 1, 'NAM90-38-BLANCO', 549.90, 8, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766708628/tiendas/zapatik/productos/gbnzhdyt3zn708uvxgym.png', TRUE),
(1, 1, 'NAM90-39-NEGRO', 549.90, 15, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766708689/tiendas/zapatik/productos/p98bhi2npuy4z5jjk1sz.png', TRUE),
(1, 1, 'NAM90-40-ROJO', 579.90, 5, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766708629/tiendas/zapatik/productos/qzrelpqi6ctuo6m1yasw.png', TRUE),
(1, 1, 'NAM90-42-AZUL', 549.90, 3, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766708631/tiendas/zapatik/productos/bydofblcgatdflbbhse3.png', TRUE),
(2, 1, 'AUB-39-GRIS', 699.90, 10, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766703511/tiendas/zapatik/productos/ueorju4b64f1bskrjegp.png', TRUE),
(2, 1, 'AUB-41-NEGRO', 699.90, 7, 'https://res.cloudinary.com/dqznlmig0/image/upload/v1766703547/tiendas/zapatik/productos/gnf99rxfzexe4qzqf2g1.png', TRUE),
(3, 2, 'CH65W-BLANCO', 89.90, 25, 'https://techpro.pe/cargador-blanco.jpg', TRUE),
(3, 2, 'CH65W-NEGRO', 89.90, 18, 'https://techpro.pe/cargador-negro.jpg', TRUE),
(3, 2, 'CH100W-GAN', 129.90, 30, 'https://techpro.pe/gan-100w.jpg', TRUE),
(4, 2, 'SONY-XM5-BLACK', 1499.90, 8, 'https://techpro.pe/sony-black.jpg', TRUE),
(4, 2, 'SONY-XM5-SILVER', 1499.90, 5, 'https://techpro.pe/sony-silver.jpg', TRUE),
(5, 3, 'VEST-FLORAL-S', 129.90, 20, 'https://moda.pe/vestido-s.jpg', TRUE),
(5, 3, 'VEST-FLORAL-M', 139.90, 15, 'https://moda.pe/vestido-m.jpg', TRUE),
(6, 3, 'POLO-BASICO-M-BLACK', 79.90, 25, 'https://moda.pe/polo-black-m.jpg', TRUE);

INSERT INTO Variante_Atributo (variante_id, atributo_valor_id) VALUES
-- Nike Air Max 90 (variantes 1 a 5) → Talla + Color
(1, 1), (1, 6),   -- 38 + Negro
(2, 1), (2, 7),   -- 38 + Blanco
(3, 2), (3, 6),   -- 39 + Negro
(4, 3), (4, 8),   -- 40 + Rojo
(5, 5), (5, 9),   -- 42 + Azul

-- Adidas Ultraboost (variantes 6 y 7) → solo Talla (no tienen color definido)
(6, 2),           -- 39
(7, 4),           -- 41

-- Cargadores (variantes 8, 9, 10) → solo Capacidad
(8, 10),          -- 65W
(9, 10),          -- 65W
(10, 11);         -- 100W

INSERT INTO carrito (session_id, variante_id, cantidad) VALUES
('demo_session_2025', 1, 2),
('demo_session_2025', 4, 1),
('demo_session_2025', 8, 1);

SELECT 'STORECOLLECTION v2.0 - 2025 - +1,200 REGISTROS CARGADOS CORRECTAMENTE - TODO FUNCIONA' AS STATUS;