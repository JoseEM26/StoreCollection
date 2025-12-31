-- ========================================================
-- STORECOLLECTION v2.2 - SCRIPT CORREGIDO Y CON ORDEN ADECUADO (27 DIC 2025)
-- ========================================================

DROP DATABASE IF EXISTS tienda_saas;
CREATE DATABASE tienda_saas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda_saas;

-- ========================================
-- TABLAS SIN DEPENDENCIAS (primero)
-- ========================================

CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    activo BOOLEAN DEFAULT true NOT NULL,
    password VARCHAR(255) NOT NULL,
    celular VARCHAR(15),
    rol ENUM('ADMIN', 'OWNER', 'CUSTOMER') DEFAULT 'CUSTOMER'
);

CREATE TABLE plan (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(60) NOT NULL,
    slug                VARCHAR(50) NOT NULL UNIQUE,
    descripcion         TEXT,
    precio_mensual      DECIMAL(10,2) DEFAULT 0.00,
    precio_anual        DECIMAL(10,2) DEFAULT NULL,
    intervalo_billing   VARCHAR(20) NOT NULL DEFAULT 'month',
    intervalo_cantidad  INT NOT NULL DEFAULT 1,                -- CORREGIDO: SMALLINT → INT
    duracion_dias       INT DEFAULT NULL,
    max_productos       INT NOT NULL DEFAULT 100,
    max_variantes       INT DEFAULT 500,
    es_trial            BOOLEAN DEFAULT FALSE,
    dias_trial          SMALLINT DEFAULT 0,
    es_visible_publico  BOOLEAN DEFAULT TRUE,
    orden               SMALLINT DEFAULT 999,
    activo              BOOLEAN DEFAULT TRUE NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_slug (slug)
);

-- ========================================
-- TABLAS QUE SON REFERENCIADAS
-- ========================================

CREATE TABLE tienda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(120) UNIQUE NOT NULL,
    whatsapp VARCHAR(20),
    moneda VARCHAR(20) DEFAULT 'SOLES' NOT NULL,
    descripcion TEXT,
    direccion TEXT,
    horarios TEXT,
    mapa_url TEXT,
    logo_img_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    user_id INT NOT NULL,
    plan_id INT NOT NULL,
    email_remitente      VARCHAR(150)          NULL,          -- ej: dueno@mitienda.com
    email_app_password   VARCHAR(255)          NULL,          -- ¡Nunca en texto plano en producción!
    FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES plan(id) ON DELETE CASCADE
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
    session_id VARCHAR(100),              -- Para asociar con carrito si es guest
    user_id INT,                          -- Opcional (si está logueado)
    tienda_id INT NOT NULL,               -- Multi-tenant

    total DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,

    estado ENUM('PENDIENTE', 'ATENDIDA', 'CANCELADA') NOT NULL DEFAULT 'PENDIENTE',

    -- Nuevos campos de comprador y envío
    comprador_nombre    VARCHAR(100) NOT NULL,
    comprador_email     VARCHAR(120) NOT NULL,
    comprador_telefono  VARCHAR(20) DEFAULT NULL,

    direccion_envio     VARCHAR(150) NOT NULL,
    referencia_envio    VARCHAR(100) DEFAULT NULL,
    distrito            VARCHAR(60) NOT NULL,
    provincia           VARCHAR(60) NOT NULL,
    departamento        VARCHAR(40) NOT NULL,
    codigo_postal       VARCHAR(10) DEFAULT NULL,

    tipo_entrega ENUM('DOMICILIO', 'RECOGIDA_EN_TIENDA', 'AGENCIA')
        NOT NULL DEFAULT 'DOMICILIO',

    -- Claves foráneas
    FOREIGN KEY (user_id)   REFERENCES usuario(id)   ON DELETE SET NULL,
    FOREIGN KEY (tienda_id) REFERENCES tienda(id)    ON DELETE CASCADE,

    -- Índices recomendados (mejoran consultas frecuentes)
    INDEX idx_tienda_estado (tienda_id, estado),
    INDEX idx_session_id (session_id),
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE boleta_detalle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    boleta_id INT NOT NULL,
    variante_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,  -- Precio al momento de compra
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (boleta_id) REFERENCES boleta(id) ON DELETE CASCADE,
    FOREIGN KEY (variante_id) REFERENCES producto_variante(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;