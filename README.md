# SkillPilot AI Roadmap Generator

SkillForge is a full-stack AI learning planner that turns a user's goal, experience level, and learning style into a personalized roadmap with trackable milestones, direct resources, and progress monitoring.

## Overview

SkillForge is built for learners who want a structured path instead of random tutorials. The app generates a roadmap, stores it per user, and lets users track completion module by module.

## Core Features

- JWT-based authentication
- Personalized roadmap generation from user input
- User-owned saved roadmaps
- Module completion tracking and progress stats
- Free and paid learning resources per milestone
- Provider fallback chain:
  - Gemini
  - additional Gemini keys
  - OpenRouter free backup
  - local failsafe generation
- Server-side resource URL normalization and repair

## Tech Stack

### Frontend

- React
- Axios
- Custom CSS design system

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- RestTemplate
- JJWT

### Database

- MySQL

### AI Layer

- Google Gemini API
- OpenRouter free fallback
- Local failsafe roadmap generator

## Project Structure

```text
backend/   Spring Boot REST API
frontend/  React application
```

## How It Works

1. The user signs up or logs in.
2. The frontend sends the roadmap goal, level, and learning preference.
3. The backend tries Gemini first.
4. If Gemini is unavailable, it rotates through backup providers.
5. The roadmap is saved to MySQL with user ownership.
6. The frontend renders the modules and tracks completion progress.

## Setup

### Requirements

- Java 17
- Node.js 18+
- MySQL 8+
- Maven 3.9+

### Backend Setup

Copy the local config template:

```bat
copy backend\local-dev.cmd.example backend\local-dev.cmd
```

Set these values in `backend/local-dev.cmd`:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `GEMINI_API_KEY` or `GEMINI_API_KEYS`
- `OPENROUTER_API_KEY` if you want the cloud fallback layer

Reference files:

- [backend/local-dev.cmd.example](backend/local-dev.cmd.example)
- [backend/.env.example](backend/.env.example)

### Frontend Setup

Inside `frontend/.env`:

```env
REACT_APP_API_URL=http://localhost:8080
```

Reference:

- [frontend/.env.example](frontend/.env.example)

### Database Setup

Create the database once:

```sql
CREATE DATABASE skillforge;
```

Hibernate uses `ddl-auto=update`, so tables are created automatically on first run.

### Run the App

#### Start the backend

```bat
start-backend.cmd
```

#### Start the frontend

```bat
start-frontend.cmd
```

Then open:

```text
http://localhost:3000
```

## Main API Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /ai/generate-roadmap`
- `GET /ai/roadmaps`
- `GET /modules/{roadmapId}`
- `PUT /modules/{id}/complete`
- `GET /progress/{roadmapId}`

## Security Notes

- JWT is stored in `localStorage` as `sf_token`
- All non-`/auth/**` endpoints are protected
- Roadmaps and modules are validated against the logged-in user

## Repository Notes

- Keep secrets in `backend/local-dev.cmd`
- Do not commit real API keys
- Build output, local logs, and local tools are ignored through `.gitignore`

## Future Improvements

- Verified resource discovery from trusted domains
- Better provider quality scoring
- Roadmap editing and sharing
- Deployment-ready environment setup
