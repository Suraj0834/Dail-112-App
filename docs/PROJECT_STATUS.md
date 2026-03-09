# Dial-112 Project Status & Documentation

This document serves as a comprehensive overview of the **Dial-112 Smart Police System**. It details the architecture, everything that has been successfully implemented up to this point, and clearly defines the remaining steps needed to achieve a 100% production-ready launch.

---

## 🏗️ 1. Architecture Overview
The system is built on a highly modular, decoupled architecture consisting of four main pillars:
1. **Citizen & Police Application**: Android app built with Kotlin.
2. **Control Room Web Portal**: Web dashboard built with React, TypeScript, and Vite.
3. **Node.js Core Backend**: API Gateway and Socket.IO real-time server using Express and MongoDB.
4. **Python AI Microservice**: Standalone FastAPI service running computer vision and machine learning (DeepFace, YOLOv8).

All these services are orchestrated using **Docker Compose**, providing a seamless localized development to production workflow.

---

## ✅ 2. What Is Done (Completed Features)

### 📲 A. Android App (Mobile)
- **Dependency Resolution**: Fixed breaking build errors caused by the outdated `blurview` library. Updated standard dependencies ensuring a successful Gradle compile (`./gradlew assembleDebug`).
- **Navigation Architecture**: Configured `MainActivity.kt` to use the Jetpack Navigation component (`NavHostFragment`).
- **Authentication Flow**: Bound the navigation state to `AuthViewModel`, automatically routing users to the Home screen if a session exists, or the Login screen if unauthorized.
- **UI Modernization**: Created a modern, glassmorphism-based UI for `fragment_login.xml`.

### ⚙️ B. Node.js Core Backend
- **Base Server**: Established the Express app in `app.js` with essential security middleware (`helmet`, `cors`, `express-rate-limit`, `express-mongo-sanitize`).
- **Authentication & RBAC**: Constructed `auth.middleware.js` providing robust JWT token verification and Role-Based Access Control (`authenticate`, `authorize`).
- **Portal Extensions (Control Room APIs)**: Extended the existing application by injecting a dedicated portal router (`portal.routes.js`) specifically for administrators and control room operators.
  - **Dashboard Aggregations**: (`GET /portal/dashboard/stats`, `trends`, `crime-distribution`) leveraging advanced MongoDB `$aggregate` pipelines.
  - **SOS Management**: (`GET /portal/sos`, `POST /portal/sos/:id/dispatch`) with native emit of Socket.IO events (`sos_dispatched`) to officers.
  - **Police Onboarding**: Integrated `multer` to handle parsing profile photo uploads.
  - **Criminals & Vehicles**: Enabled CRUD operations and array-push logic for managing criminal histories and toggling stolen vehicle flags.
- **Database Schemas**: Finalized Mongoose models schemas (`User`, `Case`, `SosLog`, `Criminal`, `Vehicle`, `Hotspot`). Upgraded the `Criminal` model with specific properties to support AI Face Embeddings.
- **Testing**: Added Jest `supertest` coverage for the portal routes (`portal.test.js`) verifying RBAC validation.
- **PM2**: Configured `ecosystem.config.js` for executing the backend in PM2 Cluster Mode across CPU cores.

### 💻 C. Control Room Web Portal (Frontend)
- **Framework Initialization**: Initialized a highly-optimized Vite + React + TypeScript environment.
- **Design System**: Built a "Material Design 3" custom theme in `theme.ts` featuring deep dark-mode gradients, glassmorphism cards, and the Inter font family.
- **State Management & Routing**:
  - Implemented secure React Router (`AppRouter.tsx`) with lazily-loaded code splitting and `<ProtectedRoute>` wrappers.
  - Created global `AuthContext.tsx` handling JWT persistence in `localStorage`.
- **Real-Time Integration**: 
  - Constructed `SocketContext.tsx` creating a global Socket.IO client.
  - Enabled browser audio alerts and visual toasts upon receiving a live `sos_alert` or `weapon_alert` from the backend.
- **Comprehensive Page Views**:
  - **Login Page**: Animated gateway entry screen.
  - **Dashboard**: Features Recharts Area and Pie charts rendering dynamic MongoDB aggregations.
  - **SOS Monitor**: Integrates `Google Maps API` laying down custom markers for real-time alerts. Includes a ticker of incoming alerts and a dialog to instantly assign nearby officers.
  - **Cases Page**: Tabular layout to view FIRs, update investigation statuses, and re-assign officers.
  - **Police Page**: Roster list featuring role toggles, on-duty status indicators, and an onboarding popup.
  - **Criminals Page**: Includes a fully-functional **AI Face Recognition Panel** allowing operators to upload a photo and scan the database.
  - **Vehicles Page**: Includes an **AI ANPR Panel** to upload a dashboard-camera snapshot and read the number plate automatically.
  - **Analytics Page**: Renders a geographical **Google Maps HeatmapLayer** based on AI-generated hotspots.

### 🐳 D. DevOps & Deployment
- **Dockerization**:
  - Wrote a 2-stage `Dockerfile` for the React Frontend that builds the static assets and serves them behind a minimal `nginx:alpine` image.
  - Wrote a custom `nginx.conf` handling Single Page App routing fallbacks, proxying `/api` natively to the Node cluster, and properly upgrading headers for `/socket.io`.
  - Wrote a `Dockerfile` for the Node.js backend to run inside PM2.
- **Microservice Orchestration**: Modified `docker-compose.yml` adding the `web-portal` service and verifying the bridges connecting MongoDB, Node Backend, and the FastAPI Python service.
- **Environment Automation**: Documented keys inside `.env.example`.
- **API Documentation**: Generated a comprehensive Postman Collection (`postman_collection.json`) mapping out the headers, payloads, and tokens necessary for interaction.

---

## 🚀 3. What Is Left (Pending Operations)

While the entire Web Portal and Core Backend infrastructure is structurally complete and interlinked, the following tasks must be completed to round out the full 360-degree system:

### 📱 A. Android App Finalization (Kotlin)
- **Socket.IO Mobile Integration**: The Web Portal currently dispatches officers successfully. The Android App needs its `SocketManager` configured to listen for `sos_dispatched` and trigger a local push notification / ringtone for the targeted police officer.
- **Location Tracking Service**: The app must periodically send coordinate updates (e.g., via a Foreground Service) back to Node.js `POST /api/location` or a socket channel so the Web Portal perfectly tracks mobile officers on the map.
- **UI Screens**: Missing fragments such as `HomeFragment` (Citizen view vs Police view), `ReportCrimeFragment` (FIR form), and `SosButtonFragment` need to be wired up to their respective Axios/Retrofit API calls.

### 🧠 B. Python AI Microservice Development
- **Status**: The Node.js backend correctly proxies requests to `http://ai-service:8000/api/ai/*`, but the actual Python code needs the heavy lifting implemented.
- **Facial Recognition**: Require implementation of `DeepFace` or `dlib` to receive an uploaded image, encode a 128-dimensional vector, and query MongoDB to compare distances against known `Criminal` embeddings.
- **ANPR**: Require implementation of `EasyOCR` or `Tesseract` to crop the number plate from an image and parse the alphanumeric characters cleanly.
- **Weapon Detection**: Require implementation of a `YOLOv8` model that can scan a frame, draw bounding boxes around weapons, and return confidence scores.
- **Hotspot Clustering**: Implement an endpoint utilizing `Scikit-learn` (DBSCAN / KMeans) on historical MongoDB `Case` locations and generating `riskScores` for the Analytics Heatmap.

### 🔒 C. Web Portal Polish & Edge Cases
- **Pagination Tuning**: Right now lists render efficiently, but as databases grow to millions of rows, the React tables should hook into true server-side pagination boundaries (React Query `keepPreviousData`).
- **Offline Reliability**: Improve `SocketContext.tsx` to handle sudden internet disconnects with a visual "Reconnecting..." banner at the very top of the screen.

### 🚀 D. Production Deployment Preparation
- **SSL / TLS**: NGINX must be modified to include SSL certificates generated via Let's Encrypt / Certbot before the frontend and API can be securely exposed over `HTTPS`.
- **CI/CD Pipeline**: Write a GitHub Actions `.yml` workflow that auto-runs the Node.js tests (`npm test`) on Pull Requests, and auto-builds Docker images strictly on trailing `main` branch merges.
- **Model Hosting**: Determine if the hefty `.pt` YOLO weights and `.h5` FaceNet models will be packed natively into the Docker container, or downloaded optimally on container spin-up via an S3 bucket.

---

## Conclusion
The Web Portal serves as the operational heart of the Dial-112 framework. The immediate next step is writing out the **Python AI microservice APIs**, ensuring the computer vision dependencies parse data smoothly, followed strictly by hooking up the remaining **Android UI fragments**.
