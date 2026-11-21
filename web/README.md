# Coworking Hub — web app

A responsive web front end for the Coworking-Hub API: browse spaces on a map, compare room prices, book with a live price estimate, and manage spaces and booking requests as an owner.

React 18 + TypeScript (strict) + Vite, React Router, TanStack Query for server data, zod to validate every API response at runtime, Leaflet with OpenStreetMap tiles. Light and dark themes follow the system setting.

## Run it

The app expects the Spring Boot API on `http://localhost:8081` (see the root README). `/api` requests are proxied there, so the browser only ever talks to one origin.

```bash
npm install
npm run dev
```

Open http://localhost:5173. To point at another API: `API_URL=http://localhost:9000 npm run dev`.

Production build, served with the same proxy:

```bash
npm run build
npm run preview
```

In production, serve `dist/` and the API from the same origin (for example a reverse proxy that sends `/api/*` to Spring Boot and everything else to `dist/index.html`).

## Demo data

With the API running, fill it with six spaces in Tunis, rooms, equipment and bookings in every state:

```bash
npm run seed
```

All demo accounts use the password `demo-pass-123`:

| Role | Email |
|---|---|
| Owner | `sana@demo.coworkinghub.tn`, `karim@demo.coworkinghub.tn` |
| Member | `youssef@demo.coworkinghub.tn`, `leila@demo.coworkinghub.tn` |

The seed goes through the public API, so it is safe to re-run.

## Checks

```bash
npm run typecheck
npm test
```

## Screenshots and walkthrough video

`npm run demo` drives the running app (after `npm run seed` and `npm run preview`) with Playwright and writes screenshots plus `walkthrough.webm` to `OUT` (default `demo-output/`). Playwright is not a project dependency: install it once with `npm i -g playwright && npx playwright install chromium`.

## Layout

| Path | What |
|---|---|
| `src/api/` | Fetch client (problem-details errors, token header), zod schemas, query and mutation hooks |
| `src/auth/` | Session context (token in `localStorage`) and route guard |
| `src/components/` | Layout, map, booking form, request item, shared states |
| `src/pages/` | Explore, space detail, login/sign-up, my bookings, owner dashboard, space management, profile |
| `src/lib/` | Price estimate (mirrors the backend rule) and formatting |

The price shown before booking is an estimate; the server decides the final room and price, and answers `409` when every fitting room is taken for that slot.
