# DeadlineShield frontend

The Next.js App Router dashboard for DeadlineShield.

## Run locally

From `frontend/`, install dependencies and start the development server:

```bash
npm ci
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The dashboard expects the Spring Boot API at `http://localhost:8080`.

Set `NEXT_PUBLIC_API_BASE_URL` to override the API base URL. The backend accepts the frontend origin from `FRONTEND_ORIGIN` (defaults to `http://localhost:3000`).

## Project structure

- `app/` contains App Router routes and shared styles.
- `components/` contains dashboard UI.
- `services/` contains the typed Spring Boot API client.
- `types/` contains shared TypeScript contracts.
