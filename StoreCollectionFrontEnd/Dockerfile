# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

# Copiar package.json y package-lock.json
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copiar todo el proyecto
COPY . .

# Build Angular producción
RUN npm run build -- -- --project StoreCollectionFrontEnd --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar contenido viejo
RUN rm -rf ./*

# Copiar el contenido correcto del build Angular
COPY --from=build /app/dist/store-collection-front-end/ /usr/share/nginx/html/

EXPOSE 80

# Ejecutar NGINX en foreground
CMD ["nginx", "-g", "daemon off;"]
