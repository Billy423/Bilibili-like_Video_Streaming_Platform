# Bilibili Project DevOps Commands

.PHONY: help build test clean start stop restart logs shell

# Default target
help:
	@echo "Bilibili Project DevOps Commands"
	@echo "================================="
	@echo "build          - Build the application"
	@echo "test           - Run tests"
	@echo "clean          - Clean build artifacts"
	@echo "dev            - Start development environment"
	@echo "prod           - Start production environment"
	@echo "stop           - Stop all services"
	@echo "restart        - Restart all services"
	@echo "logs           - Show logs from all services"
	@echo "logs-api       - Show logs from API service"
	@echo "shell          - Open shell in API container"
	@echo "db-shell       - Open MySQL shell"
	@echo "redis-cli      - Open Redis CLI"
	@echo "clean-docker   - Clean Docker resources"

# Build the application
build:
	mvn clean package -DskipTests

# Run tests
test:
	mvn test

# Clean build artifacts
clean:
	mvn clean
	docker system prune -f

# Development environment
dev:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build

# Production environment
prod:
	docker-compose up --build -d

# Stop all services
stop:
	docker-compose down

# Restart all services
restart: stop prod

# Show logs from all services
logs:
	docker-compose logs -f

# Show logs from API service only
logs-api:
	docker-compose logs -f bilibili-api

# Open shell in API container
shell:
	docker-compose exec bilibili-api sh

# Open MySQL shell
db-shell:
	docker-compose exec mysql mysql -u bilibili -p demo

# Open Redis CLI
redis-cli:
	docker-compose exec redis redis-cli

# Clean Docker resources
clean-docker:
	docker-compose down -v
	docker system prune -a -f
	docker volume prune -f
