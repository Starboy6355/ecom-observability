# ShopSmart — E-Commerce Observability Platform

A production-grade microservices application built and instrumented for enterprise observability using Dynatrace, OpenTelemetry, Prometheus, and Grafana.

---

## Architecture

```
                    ┌─────────────────────┐
    User/manual load ───►│    API Gateway      │ :8080
                    │   (Spring Boot)     │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │    Order     │  │  Inventory   │  │ Notification │
    │   Service   │  │   Service    │  │   Service    │
    │    :8081    │  │    :8082     │  │    :8083     │
    └──────────────┘  └──────────────┘  └──────────────┘
           │                 │                 │
           └─────────────────┴─────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  OTel Collector  │
                    │  (gateway mode) │
                    └────────┬────────┘
                             │
                ┌────────────┴────────────┐
                ▼                         ▼
         Dynatrace SaaS             Prometheus
         (APM + Davis AI)          (open source)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Container | Docker |
| Orchestration | Kubernetes (k3s) |
| Package Manager | Helm |
| Instrumentation | OpenTelemetry SDK |
| Telemetry Pipeline | OTel Collector (agent + gateway) |
| APM | Dynatrace OneAgent |
| Metrics | Prometheus + Micrometer |
| Dashboards | Grafana |
| Load Testing | Apache JMeter |
| Cloud | AWS EC2 (m7i-flex.large) |

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| API Gateway | 8080 | Single entry point, request routing |
| Order Service | 8081 | Order lifecycle management |
| Inventory Service | 8082 | Stock management |
| Notification Service | 8083 | Order confirmations |
| OTel Collector | 4317/4318 | Telemetry pipeline |

---

## Project Structure

```
ecom-observability/
├── services/
│   ├── api-gateway/          # Spring Boot API Gateway
│   ├── order-service/        # Order management
│   ├── inventory-service/    # Inventory management
│   └── notification-service/ # Notifications
├── helm/
│   ├── api-gateway/          # Custom Helm chart
│   ├── order-service/        # Custom Helm chart
│   ├── inventory-service/    # Custom Helm chart
│   └── notification-service/ # Custom Helm chart
├── observability/
│   └── otel-collector-config.yaml  # OTel pipeline config
├── k8s/
│   ├── namespace.yaml        # Kubernetes namespace
│   └── otel-collector.yaml   # OTel Collector deployment
├── build-and-push.sh         # Build + push Docker images
├── deploy.sh                 # Deploy to Kubernetes
└── README.md
```

---

## Prerequisites

- AWS EC2 Ubuntu 24.04 (m7i-flex.large or larger)
- Docker installed
- k3s installed
- Helm v3 installed
- Docker Hub account
- Dynatrace free trial account

---

## Setup

### Step 1 — Clone the repo
```bash
git clone https://github.com/Starboy6355/ecom-observability.git
cd ecom-observability
```

### Step 2 — Install Docker and k3s
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu && newgrp docker
curl -sfL https://get.k3s.io | sh -
```

### Step 3 — Install Helm
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### Step 4 — Docker Hub login
```bash
docker login
```

### Step 5 — Build and push images
```bash
chmod +x build-and-push.sh
./build-and-push.sh YOUR_DOCKERHUB_USERNAME
```

### Step 6 — Deploy to Kubernetes
```bash
chmod +x deploy.sh
./deploy.sh YOUR_DOCKERHUB_USERNAME
```

### Step 7 — Install Dynatrace
```bash
export DT_ENVIRONMENT="https://YOUR_TENANT.apps.dynatrace.com"
export DT_PLATFORM_TOKEN="YOUR_TOKEN"
source <(curl -sSL https://raw.githubusercontent.com/dynatrace-oss/dtwiz/main/scripts/install.sh)
dtwiz install kubernetes
```

### Step 8 — Expose API Gateway
```bash
kubectl port-forward svc/api-gateway 8080:8080 -n ecom --address 0.0.0.0 &
```

---

## API Endpoints

```bash
# Health check
curl http://YOUR_IP:8080/api/health

# Create order
curl -X POST http://YOUR_IP:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Laptop","quantity":1,"amount":75000,"customerId":"CUST001"}'

# Get order
curl http://YOUR_IP:8080/api/orders/ORD001

# Check inventory
curl http://YOUR_IP:8080/api/inventory/Laptop
```

---

## Observability Features

- Distributed traces across 4 services
- Custom business metrics (orders created, stock levels, notifications sent)
- Structured logging with trace correlation
- OTel Collector pipeline with sampling, filtering, enrichment
- Dynatrace OneAgent auto-instrumentation
- Davis AI problem detection
- Management zones per service
- JMeter load testing scenarios

---

## Load Testing

Use JMeter with these scenarios:
- Normal load: 10 users, 60 seconds
- Stress test: 100 users, 60 seconds
- Spike test: 200 users sudden burst
- Error scenario: hit invalid order IDs

---

## License
MIT
