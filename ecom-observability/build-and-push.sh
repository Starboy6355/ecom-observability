#!/bin/bash
# Build all 4 services and push to Docker Hub
# Usage: ./build-and-push.sh YOUR_DOCKERHUB_USERNAME

DOCKERHUB_USERNAME=$1

if [ -z "$DOCKERHUB_USERNAME" ]; then
  echo "Usage: ./build-and-push.sh YOUR_DOCKERHUB_USERNAME"
  exit 1
fi

echo "Building and pushing all services for: $DOCKERHUB_USERNAME"

SERVICES=("api-gateway" "order-service" "inventory-service" "notification-service")

for SERVICE in "${SERVICES[@]}"; do
  echo ""
  echo "========================================="
  echo "Building: $SERVICE"
  echo "========================================="

  cd services/$SERVICE

  # Build Docker image
  docker build -t $DOCKERHUB_USERNAME/$SERVICE:1.0.0 .

  if [ $? -ne 0 ]; then
    echo "Build failed for $SERVICE"
    exit 1
  fi

  # Push to Docker Hub
  docker push $DOCKERHUB_USERNAME/$SERVICE:1.0.0

  if [ $? -ne 0 ]; then
    echo "Push failed for $SERVICE"
    exit 1
  fi

  echo "$SERVICE pushed successfully"
  cd ../..
done

echo ""
echo "All services built and pushed successfully!"
echo ""
echo "Next step - update values.yaml in each helm chart with your Docker Hub username:"
echo "  repository: $DOCKERHUB_USERNAME/api-gateway"
