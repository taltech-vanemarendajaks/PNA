import { createRootRoute, Link, Outlet } from "@tanstack/react-router";

export const Route = createRootRoute({
  component: RootLayout,
});

function RootLayout() {
  return (
    <div className="min-h-screen text-base-content">
      <div className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="navbar rounded-box border border-base-300/80 bg-base-100/90 px-4 shadow-sm backdrop-blur">
          <div className="flex-1">
            <Link to="/" className="text-lg font-semibold tracking-[0.18em] text-primary">
              PNA WEB
            </Link>
          </div>
          <nav className="flex gap-2">
            <Link to="/" className="btn btn-ghost btn-sm">
              Overview
            </Link>
            <Link to="/about" className="btn btn-ghost btn-sm">
              Stack
            </Link>
          </nav>
        </header>

        <main className="flex-1 py-6 sm:py-10">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
