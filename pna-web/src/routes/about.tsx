import { createFileRoute } from "@tanstack/react-router";

const stack = [
  "Vite React TypeScript scaffolded with pnpm",
  "TanStack Router file-based routes",
  "TanStack Query provider and sample cached query",
  "DaisyUI components on Tailwind CSS v4",
  "Biome replacing ESLint and Prettier",
  "vite-plugin-pwa manifest and service worker registration",
];

export const Route = createFileRoute("/about")({
  component: AboutRoute,
});

function AboutRoute() {
  return (
    <section className="rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5">
      <span className="badge badge-secondary mb-4">Included in this scaffold</span>
      <h1 className="text-4xl font-semibold sm:text-5xl">Stack reference</h1>
      <p className="mt-4 max-w-2xl text-base-content/70 sm:text-lg">
        Each requested tool is wired in with the smallest compatible setup so the project is ready
        for development, build, and PWA preview.
      </p>

      <div className="mt-8 grid gap-4 md:grid-cols-2">
        {stack.map((item) => (
          <article key={item} className="rounded-2xl border border-base-300 bg-base-200/70 p-5">
            <p className="text-base leading-7">{item}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
