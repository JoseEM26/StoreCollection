# ================= BUILD FRONTEND =================
FROM node:18 AS build
WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .
RUN npm run build -- --project StoreCollectionFrontEnd --configuration production

# ================= RUN IN NGINX =================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=build /app/dist/store-collection-front-end/browser/ /usr/share/nginx/html/

# Copiar nginx custom
COPY ./StoreCollectionFrontEnd/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
