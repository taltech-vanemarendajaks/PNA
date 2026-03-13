# PNA Frontend Application

# Tech Stack

This project uses Vite, React 19, and TypeScript as the application foundation.
Routing is handled by TanStack Router, async state is handled by TanStack Query,
UI examples use DaisyUI on top of Tailwind CSS v4, PWA support is provided by
`vite-plugin-pwa`, and code quality is handled with Biome.

# Project Structure

The main application entry is `src/main.tsx`, which mounts the React app and
registers the PWA service worker. Routing is defined in `src/routes/`, shared
router setup lives in `src/router.tsx`, the generated TanStack route tree lives
in `src/routeTree.gen.ts`, and global styling is in `src/index.css`.

# Routing

Routing uses TanStack Router file-based routes. The root layout is defined in
`src/routes/__root.tsx`, the home route is `src/routes/index.tsx`, and the
secondary example route is `src/routes/about.tsx`.

# Getting Started

Make sure you have `pnpm` installed before running the commands below.

## Install Dependencies

```bash
pnpm install
```

## Start Development Server

```bash
pnpm dev
```

# Testing

There is currently no dedicated test runner or `test` script configured in
`package.json`.


## Run Tests

```bash
# No test script is configured yet.
```

# Code Quality

Biome is used for formatting and lint-style checks in this project.

## Run Linting

```bash
pnpm lint
```

## Fix Lint Issues

```bash
pnpm exec biome check --write .
```

## Format Code

```bash
pnpm format
```

# Progressive Web App (PWA)

The app is configured as a Progressive Web App with `vite-plugin-pwa`.
Manifest metadata is configured in `vite.config.ts`, service worker
registration happens in `src/main.tsx`, and the icon assets live in `public/`.


# Available Scripts

- `pnpm dev` - starts the Vite development server
- `pnpm build` - builds the production app and runs TypeScript project checks
- `pnpm preview` - serves the production build locally
- `pnpm lint` - runs Biome checks
- `pnpm format` - formats the codebase with Biome
- `pnpm check` - runs the Biome check command
