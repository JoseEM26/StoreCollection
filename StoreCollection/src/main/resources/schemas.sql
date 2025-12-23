-- ========================================================
-- STORECOLLECTION v2.1 - SCRIPT ACTUALIZADO 2025
-- Agregadas tablas para Boleta y BoletaDetalle
-- Modificaciones para manejo de compras
-- ========================================================

-- 1. ELIMINAR Y CREAR BASE DE DATOS (mismo)
DROP DATABASE IF EXISTS tienda_saas;
CREATE DATABASE tienda_saas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda_saas;

-- ========================================
-- 2. CREACIÓN DE TABLAS (ORDEN CORRECTO)
-- ========================================

-- Tablas existentes (sin cambios mayores, solo agrego las nuevas al final)

CREATE TABLE plan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    precio DECIMAL(10,2) DEFAULT 0.00,
    activo BOOLEAN DEFAULT true NOT NULL,
    max_productos INT DEFAULT 100,
    mes_inicio INT NOT NULL CHECK (mes_inicio BETWEEN 1 AND 12),
    mes_fin INT NOT NULL CHECK (mes_fin BETWEEN 1 AND 12)
);

CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    activo BOOLEAN DEFAULT true NOT NULL,
    password VARCHAR(255) NOT NULL,
    celular VARCHAR(15),
    rol ENUM('ADMIN', 'OWNER', 'CUSTOMER') DEFAULT 'CUSTOMER'
);

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
    logo_img_url TEXT,
    activo BOOLEAN DEFAULT true NOT NULL,
    plan_id INT,
    user_id INT NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE categoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tienda_id INT NOT NULL,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    UNIQUE KEY uq_categoria_slug_tienda (slug, tienda_id)
);

CREATE TABLE producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    categoria_id INT NOT NULL,
    tienda_id INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id) ON DELETE CASCADE,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    UNIQUE KEY uq_producto_slug_tienda (slug, tienda_id)
);

CREATE TABLE producto_variante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    tienda_id INT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    precio DECIMAL(10,2) NOT NULL CHECK (precio > 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    imagen_url VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_variante_producto
        FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE CASCADE,
    CONSTRAINT fk_variante_tienda
        FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    INDEX idx_producto (producto_id),
    INDEX idx_tienda (tienda_id),
    UNIQUE INDEX uq_sku (sku),
    INDEX idx_activo (activo),
    INDEX idx_precio_stock (precio, stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    tienda_id INT NOT NULL,
    FOREIGN KEY (atributo_id) REFERENCES atributo(id) ON DELETE CASCADE,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE,
    UNIQUE KEY uq_atributo_valor (atributo_id, valor)
);

CREATE TABLE variante_atributo (
    variante_id INT NOT NULL,
    atributo_valor_id INT NOT NULL,
    PRIMARY KEY (variante_id, atributo_valor_id),
    FOREIGN KEY (variante_id) REFERENCES producto_variante(id) ON DELETE CASCADE,
    FOREIGN KEY (atributo_valor_id) REFERENCES atributo_valor(id) ON DELETE CASCADE
);

CREATE TABLE carrito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    variante_id INT NOT NULL,
    cantidad INT DEFAULT 1,
    FOREIGN KEY (variante_id) REFERENCES producto_variante(id) ON DELETE CASCADE
);

-- NUEVAS TABLAS PARA BOLETAS (COMPRAS)

CREATE TABLE boleta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),  -- Para asociar con carrito si es guest
    user_id INT,              -- Si el usuario está logueado (opcional)
    tienda_id INT NOT NULL,   -- Para multi-tenant
    total DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PENDIENTE', 'COMPLETADA', 'CANCELADA') DEFAULT 'PENDIENTE',
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE SET NULL,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id) ON DELETE CASCADE
);

CREATE TABLE boleta_detalle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    boleta_id INT NOT NULL,
    variante_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,  -- Precio al momento de compra
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (boleta_id) REFERENCES boleta(id) ON DELETE CASCADE,
    FOREIGN KEY (variante_id) REFERENCES producto_variante(id) ON DELETE RESTRICT
);