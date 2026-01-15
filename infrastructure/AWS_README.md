# Personal Hosting Guide

This guide is designed for hosting the application on your **personal infrastructure** (e.g., AWS EC2, DigitalOcean Droplet, or a home server).

## 🚫 No Enterprise/Internal Dependencies
This project is completely standalone. It uses:
- Official Docker Hub images (`postgres:15-alpine`, `node:18-alpine`, `eclipse-temurin:17-jdk-alpine`).
- Standard Maven Central dependencies.
- Standard NPM Registry packages.

## Recommended: Docker Compose (Simplest)

This is the easiest way to run the full stack on a personal server (Ubuntu/Linux).

### 1. Prepare your Server
- Launch an instance (e.g., AWS t3.medium or DigitalOcean 4GB Droplet).
- Install Docker & Docker Compose:
  ```bash
  sudo apt update
  sudo apt install docker.io docker-compose
  ```
- **Crucial**: Install Google Chrome (for Selenium Scraping)
  ```bash
  wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
  sudo apt install ./google-chrome-stable_current_amd64.deb
  ```

### 2. Deploy
Copy your project files to the server:
```bash
# From your local machine
scp -r backend frontend docker-compose.yml user@your-server-ip:~/job-app
```

### 3. Run
```bash
cd ~/job-app
# Build and start in background
sudo docker-compose up -d --build
```

### 4. Access
- Frontend: `http://your-server-ip`
- Backend API: `http://your-server-ip:8080`

## Option 2: Kubernetes (MicroK8s / Minikube)
If you prefer Kubernetes on your personal server:
1. Apply the manifests:
   ```bash
   kubectl apply -f infrastructure/deployment.yaml
   kubectl apply -f infrastructure/service.yaml
   ```
2. Note: You will need to build the docker images (`job-updater-backend`, `job-updater-frontend`) locally on the server or push them to your personal Docker Hub account first.
