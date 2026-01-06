# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

# Copiar package.json y package-lock.json
COPY package*.json ./

# Instalar dependencias ignorando conflictos de peer
RUN npm install --legacy-peer-deps

# Copiar todo el proyecto
COPY . .

# Build producción Angular
RUN npm run build -- --project StoreCollectionFrontEnd --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar cualquier contenido viejo
RUN rm -rf ./*

# Copiar el contenido del build Angular a NGINX
COPY --from=build /app/dist/store-collection-front-end/ /usr/share/nginx/html/

# Exponer puerto 80
EXPOSE 80

# Arrancar NGINX en foreground
CMD ["nginx", "-g", "daemon off;"]
