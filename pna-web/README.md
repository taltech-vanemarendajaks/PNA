# PNA Frontend

Vite + React + TypeScript frontend for PNA.

## Stack

- React 19
- TypeScript
- Vite
- TanStack Router
- TanStack Query
- Tailwind CSS v4 + DaisyUI
- `@react-oauth/google`
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
VITE_GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
VITE_API_BASE_URL=http://localhost:8080
```

Google login requirements:

- Add `http://localhost:5173` to Authorized JavaScript origins in Google Cloud.
- Use the same client ID in backend `GOOGLE_CLIENT_ID`.

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
