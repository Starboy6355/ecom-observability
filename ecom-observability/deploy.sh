#!/bin/bash
# Deploy all services to Kubernetes using Helm
# Usage: ./deploy.sh YOUR_DOCKERHUB_USERNAME

DOCKERHUB_USERNAME=$1

if [ -z "$DOCKERHUB_USERNAME" ]; then
  echo "Usage: ./deploy.sh YOUR_DOCKERHUB_USERNAME"
  exit 1
fi

echo "Deploying ecom-observability to Kubernetes"

# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy OTel Collector
kubectl apply -f k8s/otel-collector.yaml

# Deploy all services using Helm
SERVICES=("api-gateway" "order-service" "inventory-service" "notification-service")

for SERVICE in "${SERVICES[@]}"; do
  echo "Deploying $SERVICE..."
  helm upgrade --install $SERVICE ./helm/$SERVICE \
    --namespace ecom \
    --set image.repository=$DOCKERHUB_USERNAME/$SERVICE \
    --set image.tag=1.0.0
done

echo ""
echo "Deployment complete! Checking pods..."
kubectl get pods -n ecom

echo ""
echo "To expose API Gateway:"
echo "kubectl port-forward svc/api-gateway 8080:8080 -n ecom --address 0.0.0.0 &"
