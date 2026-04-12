import { MoonIcon, SunIcon } from "@heroicons/react/24/outline";
import { useEffect, useState } from "react";

type ThemeName = "dim" | "emerald";

const THEME_STORAGE_KEY = "pna.theme";

export function ThemeController() {
  const [theme, setTheme] = useState<ThemeName>(() => {
    if (typeof window === "undefined") {
      return "dim";
    }

    const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
    return storedTheme === "emerald" ? "emerald" : "dim";
  });

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  return (
    <label className="flex cursor-pointer items-center gap-2">
      <MoonIcon className="size-5 shrink-0" />
      <input
        type="checkbox"
        className="toggle"
        aria-label="Toggle theme"
        checked={theme === "emerald"}
        onChange={(event) => {
          setTheme(event.currentTarget.checked ? "emerald" : "dim");
        }}
      />
      <SunIcon className="size-5 shrink-0" />
    </label>
  );
}
