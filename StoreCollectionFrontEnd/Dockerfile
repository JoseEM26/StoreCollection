# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

# Copiar package.json para instalar deps
COPY package*.json ./
RUN npm install --legacy-peer-deps

# Copiar todo el proyecto
COPY . .

# Build producción con proyecto y configuración correctos
RUN npm run build -- --project StoreCollectionFrontEnd --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*

# Copiar build
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
