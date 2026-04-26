import { Link } from "@tanstack/react-router";

export function FooterNav() {
  return (
    <>
      <nav className="flex gap-2 items-center justify-center">
        <Link to="/privacyPolicy" className="btn btn-ghost btn-sm">
          Privacy Policy
        </Link>
        <Link to="/terms" className="btn btn-ghost btn-sm">
          Terms of Use
        </Link>
      </nav>
      <footer className="py-6 text-center text-sm text-gray-500">
        &copy; {new Date().getFullYear()} PNA WEB. All rights reserved.
      </footer>
    </>
  );
}
