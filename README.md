# Automatic Job Update

A job scraping and tracking application built with Spring Boot and React.

## Features
- Automated job scraping from various platforms (LinkedIn, Naukri, Wellfound, etc.)
- Real-time job updates via WebSockets
- Experience-based filtering (Fresher vs. Experienced)
- Application tracking (New, Applied, Draft)
- Dark-mode friendly UI with Ant Design

---

## Local Development

### Prerequisites
- Docker and Docker Compose
- Java 17 (optional, for local development without Docker)
- Node.js 18+ (optional, for local development without Docker)

### Run with Docker Compose (Recommended)
This will set up the backend, frontend, and a PostgreSQL database.

1. Clone the repository
2. Run the following command in the root directory:
   ```bash
   docker compose up --build
   ```
3. Access the application:
   - Frontend: [http://localhost](http://localhost)
   - Backend API: [http://localhost:8080](http://localhost:8080)
   - H2 Console (if using H2): [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

---

## Deployment (Render)

This project is already configured for deployment on [Render](https://render.com) using its Blueprint feature.

### Steps to Deploy
1. Push your code to a GitHub repository.
2. Log in to Render and click **New > Blueprint**.
3. Connect your GitHub repository.
4. Render will automatically detect the `render.yaml` file and create:
   - A PostgreSQL database
   - A Web Service for the backend (Docker runtime)
   - A Static Site for the frontend (Static runtime)

### Environment Variables
The following variables are automatically handled by the Blueprint but can be customized:
- `VITE_API_URL`: The URL of your deployed backend service (e.g., `https://job-backend.onrender.com`).

---

## Tech Stack
- **Backend**: Spring Boot, Spring Data JPA, Selenium, Jsoup, WebSockets (STOMP)
- **Frontend**: React (Vite), Redux Toolkit, React Query, Ant Design, TailwindCSS
- **Database**: PostgreSQL (Production), H2 (Local Development)
- **Deployment**: Docker, Nginx, Render
