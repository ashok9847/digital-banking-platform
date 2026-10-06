#!/bin/bash
# ==============================================================================
# Helper to start services pointing to local MySQL
# ==============================================================================

source ./local-env.sh

SERVICE=$1

if [ -z "$SERVICE" ]; then
    echo "Usage: ./start-services-local.sh <service-name>"
    echo ""
    echo "Available services:"
    echo "  auth-service          (Port 8081)"
    echo "  customer-service      (Port 8082)"
    echo "  account-service       (Port 8083)"
    echo "  transaction-service   (Port 8084)"
    echo "  notification-service  (Port 8085)"
    echo "  audit-service         (Port 8086)"
    echo "  api-gateway           (Port 8080)"
    echo ""
    echo "Example: ./start-services-local.sh auth-service"
    exit 1
fi

echo "Starting $SERVICE with local MySQL..."
mvn spring-boot:run -pl "$SERVICE"
