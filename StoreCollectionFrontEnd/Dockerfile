# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

# Copiar solo los archivos necesarios
COPY package*.json ./

# Instalar dependencias (ignora conflictos)
RUN npm install --legacy-peer-deps

# Copiar todo el proyecto
COPY . .

# Build en modo default
RUN npm run build

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar contenido default
RUN rm -rf ./*

# Copiar build
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
