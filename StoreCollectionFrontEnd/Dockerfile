# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

# Copiar solo los archivos necesarios para instalar dependencias
COPY package*.json ./

# Instalar dependencias forzando resolución de peer deps
RUN npm install --legacy-peer-deps

# Copiar todo el proyecto
COPY . .

# Build en modo producción
RUN npm run build --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar contenido default de NGINX
RUN rm -rf ./*

# Copiar los archivos de build desde la etapa anterior
COPY --from=build /app/dist /usr/share/nginx/html

# Exponer puerto 80 para Railway
EXPOSE 80

# Comando por defecto para NGINX
CMD ["nginx", "-g", "daemon off;"]
