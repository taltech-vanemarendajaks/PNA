export function AboutSection() {
  return (
    <section className="mx-auto w-full rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5 sm:p-10">
      <div className="max-w-3xl">
        <span className="badge badge-secondary mb-4">About</span>
        <h2 className="text-3xl font-semibold sm:text-4xl">Unknown number?</h2>
        <p className="mt-4 text-base-content/70 sm:text-lg">
          PNA is a fast way to find out who called you. You can view your search history too, which
          is why we ask you to log in.
        </p>
      </div>

      <div className="mt-8 grid gap-4 md:grid-cols-3">
        <article className="rounded-3xl border border-base-300 bg-base-200/40 p-6">
          <h3 className="text-lg font-semibold">Who is calling me?</h3>
          <p className="mt-3 text-sm leading-6 text-base-content/75">
            That is the whole idea. Search a number and get a quick, useful answer without playing
            detective first.
          </p>
        </article>

        <article className="rounded-3xl border border-base-300 bg-base-200/40 p-6">
          <h3 className="text-lg font-semibold">How does it work?</h3>
          <p className="mt-3 text-sm leading-6 text-base-content/75">
            Look up the number and PNA shows what it knows. On Android, the special app can pick up
            the number automatically, which feels suspiciously convenient. iPhone users should not
            be sad though: opening PNA as a PWA is still easier than searching from Google again.
          </p>
        </article>

        <article className="rounded-3xl border border-base-300 bg-base-200/40 p-6">
          <h3 className="text-lg font-semibold">Why did we build it?</h3>
          <p className="mt-3 text-sm leading-6 text-base-content/75">
            Because “maybe I should answer this” is not a great strategy. We wanted a faster way to
            check first and decide second.
          </p>
        </article>
      </div>
    </section>
  );
}
