#!/bin/bash

# Build Docker images
docker compose build

# Run Docker containers
docker compose up


############### Individual commands (optional to compose) #####################
# # Build the image for catalog-service
# docker build -t catalog-service ./catalogservice

# # Build the image for frontend-service
# docker build -t frontend-service ./frontendservice

# # Build the image for order-service
# docker build -t order-service ./orderservice

# docker network create my-network

# docker network create my-networkdocker run -d --name catalog-service \
#     --network my-network \
#     -v "$(pwd)/catalogservice/data:/usr/src/app/data" \
#     catalog-service

# docker run -d --name frontend-service \
#     --network my-network \
#     -p 8080:8080 \
#     -e CATALOG_HOST=catalog-service \
#     -e ORDER_HOST=order-service \
#     frontend-service

# docker run -d --name order-service \
#     --network my-network \
#     -v "$(pwd)/orderservice/logs:/usr/src/app/logs" \
#     -e CATALOG_HOST=catalog-service \
#     order-service