# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Add environment variables at build time if necessary, 
# although React handles them better if injected at runtime optionally,
# for this standard Create React App flow, they evaluate at build time.
RUN npm run build

# Production stage
FROM nginx:alpine
# Copy built assets from build stage to nginx serving folder
COPY --from=build /app/build /usr/share/nginx/html
# Also add custom nginx routing if you use client side routing (react-router)
RUN echo "server { listen 80; location / { root   /usr/share/nginx/html; index  index.html index.htm; try_files \$uri \$uri/ /index.html; } }" > /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
