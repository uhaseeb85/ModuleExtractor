# ── Stage 1: Build ──────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# ── Stage 2: Serve via Nginx ─────────────────────────────────────────
FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx config: serve SPA (all paths → index.html) and proxy /api upstream
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
