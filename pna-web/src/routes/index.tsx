import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  component: IndexRoute,
});

type AnalysisPreview = {
  coverage: string;
  successRate: string;
  lastImport: string;
  queuedChecks: number;
  carriers: string[];
};

async function getAnalysisPreview(): Promise<AnalysisPreview> {
  await new Promise((resolve) => {
    window.setTimeout(resolve, 180);
  });

  return {
    coverage: "148 countries",
    successRate: "99.2%",
    lastImport: "2,400 numbers / 16s",
    queuedChecks: 18,
    carriers: ["Telia", "Vodafone", "Orange", "AT&T"],
  };
}

function IndexRoute() {
  const preview = useQuery({
    queryKey: ["analysis-preview"],
    queryFn: getAnalysisPreview,
  });

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <section className="rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5">
        <span className="badge badge-outline badge-primary mb-4">Vite + React + TS + PWA</span>
        <h1 className="max-w-xl text-4xl font-semibold leading-tight sm:text-5xl">
          Phone number analysis with routed views and cached query state.
        </h1>
        <p className="mt-4 max-w-2xl text-base-content/70 sm:text-lg">
          This starter wires TanStack Router for navigation, TanStack Query for async data, DaisyUI
          for the interface, and Biome for linting and formatting.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/about" className="btn btn-primary">
            View stack example
          </Link>
          <a
            className="btn btn-outline"
            href="https://tanstack.com/query/latest"
            target="_blank"
            rel="noreferrer"
          >
            Query docs
          </a>
        </div>
        <div className="mt-8 grid gap-3 sm:grid-cols-3">
          <article className="rounded-2xl bg-base-200 p-4">
            <p className="text-sm uppercase tracking-[0.2em] text-base-content/50">Coverage</p>
            <p className="mt-2 text-2xl font-semibold">{preview.data?.coverage ?? "Loading..."}</p>
          </article>
          <article className="rounded-2xl bg-base-200 p-4">
            <p className="text-sm uppercase tracking-[0.2em] text-base-content/50">Success rate</p>
            <p className="mt-2 text-2xl font-semibold">
              {preview.data?.successRate ?? "Loading..."}
            </p>
          </article>
          <article className="rounded-2xl bg-base-200 p-4">
            <p className="text-sm uppercase tracking-[0.2em] text-base-content/50">Last import</p>
            <p className="mt-2 text-2xl font-semibold">
              {preview.data?.lastImport ?? "Loading..."}
            </p>
          </article>
        </div>
      </section>

      <section className="rounded-4xl border border-base-300 bg-neutral text-neutral-content shadow-xl shadow-neutral/10">
        <div className="border-b border-white/10 p-6">
          <h2 className="text-2xl font-semibold">TanStack Query example</h2>
          <p className="mt-2 text-sm text-neutral-content/70">
            Async preview data is cached under <code>analysis-preview</code>.
          </p>
        </div>
        <div className="p-6">
          {preview.isLoading ? (
            <div className="flex items-center gap-3">
              <span className="loading loading-spinner loading-sm"></span>
              <span>Loading analysis preview...</span>
            </div>
          ) : (
            <>
              <div className="stats stats-vertical mb-6 w-full bg-white/10 text-neutral-content shadow">
                <div className="stat">
                  <div className="stat-title text-neutral-content/60">Queued checks</div>
                  <div className="stat-value text-4xl">{preview.data?.queuedChecks}</div>
                </div>
              </div>
              <div>
                <p className="mb-3 text-sm uppercase tracking-[0.2em] text-neutral-content/50">
                  Carrier samples
                </p>
                <div className="flex flex-wrap gap-2">
                  {preview.data?.carriers.map((carrier) => (
                    <span key={carrier} className="badge badge-primary badge-lg">
                      {carrier}
                    </span>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </section>
    </div>
  );
}
