# PNA Frontend

Vite + React + TypeScript frontend for PNA.

## Stack

- React 19
- TypeScript
- Vite
- TanStack Router
- TanStack Query
- Tailwind CSS v4 + DaisyUI
- `vite-plugin-pwa`
- Vitest
- Biome

## Quick Start

```bash
pnpm install
cp .env.example .env
pnpm dev
```

Frontend runs at `http://localhost:5173`.

## `.env`

```env
VITE_API_BASE_URL=http://localhost:8080
```

Google login requirements:

- Configure the Google OAuth web client redirect URI as `http://localhost:8080/api/v1/auth/google/redirect`.
- Set backend `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
- Google login uses a same-window backend redirect flow; set backend `FRONTEND_BASE_URL` to the frontend origin so the browser returns to the app after authentication.

## Scripts

- `pnpm dev` - start dev server (`localhost:5173`)
- `pnpm build` - production build + typecheck
- `pnpm test` - run Vitest in watch mode
- `pnpm test:run` - run Vitest once
- `pnpm preview` - preview production build
- `pnpm pwa:preview` - production preview for PWA testing (`localhost:5173`)
- `pnpm lint` - Biome checks
- `pnpm format` - format with Biome

## Testing

This project uses Vitest for unit tests.

Use:

```bash
pnpm test
```

For a single non-watch run:

```bash
pnpm test:run
```

Test files currently follow the `src/**/*.test.ts` pattern configured in `vitest.config.ts`.

## PWA Testing

Use:

```bash
pnpm pwa:preview
```

Then open `http://localhost:5173` and test install/offline behavior.
