FROM node:18-slim

RUN apt-get update && apt-get install -y \
    chromium \
    fonts-liberation \
    libnss3 \
    libxss1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# El código usa el chromium instalado por apt (ver executablePath en index.js),
# así que se evita que puppeteer descargue su propio binario.
ENV PUPPETEER_SKIP_DOWNLOAD=true

COPY package*.json ./
RUN npm install --omit=dev

COPY index.js ./

EXPOSE 3000

CMD ["npm", "start"]
