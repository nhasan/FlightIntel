FROM node:16-alpine

ARG PM2_PUBLIC_KEY
ARG PM2_SECRET_KEY

WORKDIR /app

COPY package*.json ./

RUN npm ci --only=production --omit=dev

COPY ./ ./

RUN npm install pm2 --location=global
ENV PM2_PUBLIC_KEY $PM2_PUBLIC_KEY
ENV PM2_SECRET_KEY $PM2_SECRET_KEY

EXPOSE 8000

CMD ["pm2-runtime", "app.js"]

