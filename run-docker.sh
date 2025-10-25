#!/bin/bash

echo "Building and starting Document Analysis System..."

# Build and start services
docker-compose up --build -d

echo "Services starting..."
echo "Application will be available at: http://localhost:9080"
echo "Database will be available at: localhost:5432"

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Check if services are running
docker-compose ps

echo "Setup complete!"
echo ""
echo "API Endpoints:"
echo "- POST http://localhost:9080/api/v1/auth/register"
echo "- POST http://localhost:9080/api/v1/auth/login" 
echo "- POST http://localhost:9080/api/v1/document-analysis/analyze"
echo ""
echo "To stop: docker-compose down"