-- ========================================
-- 1. BASE DE DATOS
-- ========================================
DROP DATABASE IF EXISTS tienda_saas;
CREATE DATABASE tienda_saas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda_saas;

-- ========================================
-- 2. TABLAS
-- ========================================

-- 1. Planes
CREATE TABLE plan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) DEFAULT 0.00,
    max_productos INT DEFAULT 100,
    mes_inicio TINYINT NOT NULL CHECK (mes_inicio BETWEEN 1 AND 12),
    mes_fin TINYINT NOT NULL CHECK (mes_fin BETWEEN 1 AND 12)
);

-- 2. Usuarios
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    celular VARCHAR(15),
    rol ENUM('ADMIN', 'OWNER', 'CUSTOMER') DEFAULT 'CUSTOMER'
);

-- 3. Tiendas
CREATE TABLE tienda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) UNIQUE NOT NULL,
    whatsapp VARCHAR(20),
    moneda ENUM('SOLES', 'DOLARES') DEFAULT 'SOLES',
    descripcion TEXT,
    direccion TEXT,
    horarios TEXT,
    mapa_url TEXT,
    plan_id INT,
    user_id INT NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- 4. Categorías
CREATE TABLE categoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    tienda_id INT NOT NULL,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    UNIQUE KEY uq_categoria_slug_tienda (slug, tienda_id)
);

-- 5. Productos
CREATE TABLE producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    categoria_id INT NOT NULL,
    tienda_id INT NOT NULL,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id) ON DELETE CASCADE,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    UNIQUE KEY uq_producto_slug_tienda (slug, tienda_id)
);

-- 6. Variantes
CREATE TABLE variante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    imagen TEXT,
    FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE CASCADE
);

-- 7. Atributos y Valores
CREATE TABLE atributo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    tienda_id INT NOT NULL,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE
);

CREATE TABLE atributo_valor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    atributo_id INT NOT NULL,
    valor VARCHAR(50) NOT NULL,
    FOREIGN KEY (atributo_id) REFERENCES atributo(id) ON DELETE CASCADE,
    UNIQUE KEY uq_atributo_valor (atributo_id, valor)
);

CREATE TABLE variante_atributo (
    variante_id INT NOT NULL,
    valor_id INT NOT NULL,
    PRIMARY KEY (variante_id, valor_id),
    FOREIGN KEY (variante_id) REFERENCES variante(id) ON DELETE CASCADE,
    FOREIGN KEY (valor_id) REFERENCES atributo_valor(id) ON DELETE CASCADE
);

-- 8. Carrito
CREATE TABLE carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    variante_id INT NOT NULL,
    cantidad INT DEFAULT 1,
    FOREIGN KEY (variante_id) REFERENCES variante(id) ON DELETE CASCADE
);

-- ========================================
-- 3. DATOS DE PRUEBA
-- ========================================

-- 1. PLANES
INSERT INTO plan (nombre, precio, max_productos, mes_inicio, mes_fin) VALUES
('Gratis', 0.00, 10, 1, 12),
('Básico', 49.90, 50, 1, 12),
('Pro', 99.90, 200, 1, 12),
('Premium', 199.90, 500, 1, 12),
('Enterprise', 499.90, 9999, 1, 12);

-- 2. USUARIOS
INSERT INTO usuario (nombre, email, password, celular, rol) VALUES
('Admin Sistema', 'admin@tusaas.pe', 'admin123', '+51999999999', 'ADMIN'),

-- DUEÑOS (10)
('María López', 'maria@zapatik.pe', '123', '+51987654321', 'OWNER'),
('Carlos Ruiz', 'carlos@techpro.pe', '123', '+51911223344', 'OWNER'),
('Ana Gómez', 'ana@modafashion.pe', '123', '+51955667788', 'OWNER'),
('Luis Torres', 'luis@comidaperros.pe', '123', '+51933445566', 'OWNER'),
('Sofía Vega', 'sofia@joyeriaplata.pe', '123', '+51977889900', 'OWNER'),
('Pedro Salazar', 'pedro@deportesxtreme.pe', '123', '+51922334455', 'OWNER'),
('Laura Mendoza', 'laura@cosmeticos.pe', '123', '+51966778899', 'OWNER'),
('Diego Castro', 'diego@gadgets.pe', '123', '+51944556677', 'OWNER'),
('Valeria Ortiz', 'valeria@bellezanatural.pe', '123', '+51988990011', 'OWNER'),
('Javier Ramos', 'javier@accesorioscel.pe', '123', '+51911224455', 'OWNER'),

-- CLIENTES (5)
('Cliente 1', 'cliente1@gmail.com', '123', '+51900000001', 'CUSTOMER'),
('Cliente 2', 'cliente2@gmail.com', '123', '+51900000002', 'CUSTOMER'),
('Cliente 3', 'cliente3@gmail.com', '123', '+51900000003', 'CUSTOMER'),
('Cliente 4', 'cliente4@gmail.com', '123', '+51900000004', 'CUSTOMER'),
('Cliente 5', 'cliente5@gmail.com', '123', '+51900000005', 'CUSTOMER');

-- 3. TIENDAS
INSERT INTO tienda (nombre, slug, whatsapp, moneda, descripcion, direccion, horarios, mapa_url, plan_id, user_id) VALUES
('ZapaTik', 'zapatik', '+51987654321', 'SOLES', 'Zapatillas para TikTok viral', 'Av. Brasil 123, Lima', 'Lun-Sab 10am-8pm', 'https://maps.google.com/?q=Av+Brasil+123+Lima', 2, 2),
('TechPro', 'techpro', '+51911223344', 'DOLARES', 'Gadgets y accesorios', 'Tienda virtual', '24/7 online', 'https://maps.google.com/?q=virtual', 3, 3),
('Moda Fashion', 'moda-fashion', '+51955667788', 'SOLES', 'Ropa de tendencia', 'Jr. de la Unión 500, Lima', 'Lun-Dom 9am-9pm', 'https://maps.google.com/?q=Jr+Union+500+Lima', 2, 4),
('PetFood', 'petfood', '+51933445566', 'SOLES', 'Comida premium para mascotas', 'Av. Larco 890, Miraflores', 'Lun-Vie 8am-7pm', 'https://maps.google.com/?q=Av+Larco+890+Miraflores', 1, 5),
('Plata Joyas', 'plata-joyas', '+51977889900', 'SOLES', 'Joyería en plata 925', 'Tienda virtual', '24/7 online', 'https://maps.google.com/?q=virtual', 4, 6),
('Deportes Xtreme', 'deportes-xtreme', '+51922334455', 'SOLES', 'Ropa deportiva', 'Av. Canadá 200, La Victoria', 'Lun-Sab 10am-6pm', 'https://maps.google.com/?q=Av+Canada+200+La+Victoria', 2, 7),
('Cosméticos Naturales', 'cosmeticos-naturales', '+51966778899', 'SOLES', 'Productos orgánicos', 'Tienda virtual', '24/7 online', 'https://maps.google.com/?q=virtual', 3, 8),
('Gadgets Perú', 'gadgets-peru', '+51944556677', 'DOLARES', 'Accesorios para celular', 'Av. Primavera 800, Surco', 'Lun-Sab 11am-8pm', 'https://maps.google.com/?q=Av+Primavera+800+Surco', 2, 9),
('Belleza Natural', 'belleza-natural', '+51988990011', 'SOLES', 'Cremas y aceites', 'Tienda virtual', '24/7 online', 'https://maps.google.com/?q=virtual', 1, 10),
('Accesorios Cel', 'accesorios-cel', '+51911224455', 'SOLES', 'Carcasas y cargadores', 'Centro Comercial 123', 'Lun-Dom 10am-10pm', 'https://maps.google.com/?q=Centro+Comercial+123', 2, 11);

-- 4. CATEGORÍAS (3 por tienda)
INSERT INTO categoria (nombre, slug, tienda_id) VALUES
('Hombre', 'hombre', 1), ('Mujer', 'mujer', 1), ('Niños', 'ninos', 1),
('Cargadores', 'cargadores', 2), ('Auriculares', 'auriculares', 2), ('Carcasas', 'carcasas', 2),
('Vestidos', 'vestidos', 3), ('Polos', 'polos', 3), ('Jeans', 'jeans', 3),
('Perros', 'perros', 4), ('Gatos', 'gatos', 4), ('Accesorios', 'accesorios-mascota', 4),
('Aros', 'aros', 5), ('Collares', 'collares', 5), ('Pulseras', 'pulseras', 5),
('Camisetas', 'camisetas', 6), ('Shorts', 'shorts', 6), ('Zapatillas', 'zapatillas-deporte', 6),
('Cremas', 'cremas', 7), ('Aceites', 'aceites', 7), ('Jabones', 'jabones', 7),
('USB', 'usb', 8), ('Power Banks', 'power-banks', 8), ('Soportes', 'soportes', 8),
('Mascarillas', 'mascarillas', 9), ('Shampoo', 'shampoo', 9), ('Acondicionador', 'acondicionador', 9),
('Carcasas', 'carcasas-cel', 10), ('Vidrios', 'vidrios', 10), ('Cables', 'cables', 10);

-- 5. PRODUCTOS (ejemplo: 10)
INSERT INTO producto (nombre, slug, categoria_id, tienda_id) VALUES
('Nike Air Max 90', 'nike-air-max-90', 1, 1),
('Adidas Ultraboost', 'adidas-ultraboost', 1, 1),
('Puma RS-X', 'puma-rs-x', 2, 1),
('New Balance 574', 'new-balance-574', 3, 1),
('Converse Chuck', 'converse-chuck', 3, 1),
('Cargador 65W USB-C', 'cargador-65w', 4, 2),
('Auriculares Bluetooth', 'auriculares-bt', 5, 2),
('Carcasa iPhone 15', 'carcasa-iphone15', 6, 2),
('Cable USB-C 2m', 'cable-usbc-2m', 4, 2),
('Soporte Celular', 'soporte-celular', 6, 2);

-- 6. VARIANTES
INSERT INTO variante (producto_id, sku, precio, stock, imagen) VALUES
(1, 'NAM90-38-N', 499.90, 8, 'nike90-negro-38.jpg'),
(1, 'NAM90-40-B', 499.90, 5, 'nike90-blanco-40.jpg'),
(1, 'NAM90-42-R', 499.90, 3, 'nike90-rojo-42.jpg'),
(2, 'AUB-39-G', 599.90, 10, 'adidas-gris-39.jpg'),
(2, 'AUB-41-N', 599.90, 7, 'adidas-negro-41.jpg'),
(6, 'CH65W-W', 79.90, 20, 'cargador-blanco.jpg'),
(6, 'CH65W-B', 79.90, 15, 'cargador-negro.jpg');

-- 7. ATRIBUTOS
INSERT INTO atributo (nombre, tienda_id) VALUES
('Talla', 1), ('Color', 1),
('Tipo', 2), ('Color', 2),
('Talla', 3), ('Color', 3),
('Sabor', 4), ('Peso', 4),
('Material', 5), ('Tamaño', 5);

-- 8. VALORES DE ATRIBUTOS
INSERT INTO atributo_valor (atributo_id, valor) VALUES
(1, '38'), (1, '39'), (1, '40'), (1, '41'), (1, '42'),
(2, 'Negro'), (2, 'Blanco'), (2, 'Rojo'), (2, 'Azul'),
(3, 'USB-C'), (3, 'Lightning'), (3, 'Micro USB'),
(4, 'Blanco'), (4, 'Negro'),
(5, 'Pollo'), (5, 'Res'), (5, 'Pescado'),
(6, '1kg'), (6, '3kg'), (6, '10kg');

-- 9. UNIÓN variante_atributo
INSERT INTO variante_atributo (variante_id, valor_id) VALUES
(1, 1), (1, 6),  -- Talla 38 + Negro
(2, 3), (2, 7),  -- Talla 40 + Blanco
(3, 5), (2, 8),  -- Talla 42 + Rojo
(4, 2), (2, 9),  -- Talla 39 + Gris (agregar Gris si quieres)
(6, 10), (4, 14); -- Tipo USB-C + Color Blanco

-- 10. CARRITO
INSERT INTO carrito (session_id, variante_id, cantidad) VALUES
('sess_001', 1, 1),
('sess_001', 6, 2),
('sess_002', 2, 1),
('sess_003', 3, 1),
('sess_004', 6, 3),
('sess_005', 1, 1),
('sess_006', 2, 2),
('sess_007', 6, 1),
('sess_008', 3, 1),
('sess_009', 1, 1);