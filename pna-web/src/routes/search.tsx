import { createFileRoute } from "@tanstack/react-router";
import { redirectUnauthenticatedUser } from "../api/auth/routeAuth";
import { SearchComponent } from "../components/search-component/SearchComponent";
import { isAuthenticationError } from "../api/auth/auth";

export const Route = createFileRoute("/search")({
  beforeLoad: () => redirectUnauthenticatedUser(),
  component: SearchRoute,
});

export function getLogoutErrorMessage(
  error: unknown,
  onUnauthenticated: () => void,
): string | null {
  if (isAuthenticationError(error)) {
    onUnauthenticated();
    return null;
  }

  return error instanceof Error ? error.message : "Logout failed";
}

function SearchRoute() {
  return (
    <div className="mx-auto w-full max-w-3xl">
      <SearchComponent />
    </div>
  );
}
