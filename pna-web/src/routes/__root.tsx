import { createRootRoute, Outlet, useLocation } from "@tanstack/react-router";
import { AppNavigation } from "../components/common/AppNavigation";
import { NotFoundRoute } from "../components/common/NotFound";
import { FooterNav } from "../components/common/FooterNav";

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFoundRoute,
});

function RootLayout() {
  const location = useLocation();
  const isAuthenticatedRoute = location.pathname === "/search" || location.pathname === "/settings";
  return (
    <div className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 sm:px-6 lg:px-8 text-base-content">
      <AppNavigation isAuthenticatedRoute={isAuthenticatedRoute} />
      <main
        className={
          isAuthenticatedRoute ? "flex-1 pt-6 pb-24 sm:pt-10 md:pb-10" : "flex-1 py-6 sm:py-10"
        }
      >
        <Outlet />
      </main>
      <FooterNav />
    </div>
  );
}
