# ============================================
# 阶段1: 构建 - Node 22 编译 Vue 前端
# ============================================
FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run pure-build

# ============================================
# 阶段2: 运行 - Nginx 托管静态文件
# ============================================
FROM nginx:alpine

# 清除默认站点
RUN rm -rf /usr/share/nginx/html/*

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
