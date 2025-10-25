# Docker Setup for Document Analysis System

## Quick Start

```bash
# Start everything
./run-docker.sh

# Or manually
docker-compose up --build -d
```

## Services

- **Application**: http://localhost:9080
- **Database**: localhost:5432 (postgres/password)

## API Usage

```bash
# Register user
curl -X POST http://localhost:9080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'

# Login
curl -X POST http://localhost:9080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Analyze document
curl -X POST http://localhost:9080/api/v1/document-analysis/analyze \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@document.pdf"
```

## Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Rebuild
docker-compose up --build
```