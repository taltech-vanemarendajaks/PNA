import { Link } from "@tanstack/react-router";

export function NotFoundRoute() {
  return (
    <div className="mx-auto flex min-h-[60vh] w-full items-center justify-center">
      <section className="w-full max-w-2xl rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5 sm:p-10">
        <span className="badge badge-primary mb-4">404 Error</span>
        <h1 className="text-4xl font-semibold text-balance sm:text-5xl">
          Oopsie! Something's missing...
        </h1>
        <p className="mt-4 max-w-xl text-base text-base-content/75 sm:text-lg">
          It seems like we don't have the page you're looking for. Don't worry, it happens to the
          best of us! You can head back to the homepage and start fresh.
        </p>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
          <Link to="/" className="btn btn-primary">
            Return Home
          </Link>
        </div>
      </section>
    </div>
  );
}
