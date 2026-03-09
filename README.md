# Dial-112 Control Room Web Portal & AI Backend

A complete emergency response management system, incorporating real-time SOS alerts, AI face recognition, Automatic Number Plate Recognition (ANPR), and AI crime hotspot generation.

## Features Built
- **React Control Room Portal**: Real-time dispatch, active case management, analytics graphs (Recharts), live Google maps mapping.
- **Microservice Architecture**: Exposes Node.JS API gateway and integrates with a standalone Python FastAPI for deep learning capabilities.
- **Real-Time Engine**: Built on Socket.IO for pushing instant case updates and SOS emergency events.
- **Pre-configured Docker**: Native support for single-command `docker-compose up` setup (NGINX + PM2 Node + Python Uvicorn + MongoDB).

## Setup & Running Locally

### 1. Requirements
Ensure Docker & Docker Compose are running along with Node JS >v20 if running bare metal.

### 2. Configure Environment
Copy the example config and add Google Maps API Key:
```sh
cp .env.example .env
```
Edit `.env` to embed `VITE_GOOGLE_MAPS_KEY=YourKeyHere`

### 3. Run With Docker
Start the entire stack (MongoDB, Node API, Python AI Service, NGINX Web Portal):

```sh
docker-compose --profile dev up --build -d
```

### 4. Tests
Testing is configured in the backend package using Jest & Supertest.
```sh
cd backend
npm test
```

## Structure
- `app/` - The connected Android/Kotlin App for Users and Police
- `backend/` - The Express JS main API serving all components.
- `ai_service/` - FastAPI with scikit-learn & ultralytics doing Python AI processing.
- `web_portal/frontend/` - React/Vite/TS control room.
- `docs/` - System architecture and Postman API collection.
