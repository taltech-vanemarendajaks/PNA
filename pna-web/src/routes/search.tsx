import { createFileRoute, redirect } from "@tanstack/react-router";
import { hasStoredSession } from "../auth/session";
import { SearchComponent } from "../components/search-component/SearchComponent";

export const Route = createFileRoute("/search")({
  beforeLoad: () => {
    if (!hasStoredSession()) {
      throw redirect({ to: "/" });
    }
  },
  component: SearchRoute,
});

function SearchRoute() {
  return (
    <div className="mx-auto w-full max-w-3xl">
      <SearchComponent />
    </div>
  );
}
