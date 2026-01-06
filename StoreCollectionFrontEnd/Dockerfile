# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .

# Build producción
RUN npm run build -- --project StoreCollectionFrontEnd --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html
RUN rm -rf ./*

# Copiar la carpeta correcta del build
COPY --from=build /app/dist/StoreCollectionFrontEnd /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
