import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Icon } from "@app/ui/Icon";
import { useTheme } from "@app/components/shared/ThemeProvider";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";

function prefersDark(): boolean {
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

/**
 * The strip's light/dark toggle, portalled in from inside the app because the
 * theme lives in a provider below the strip. It always sets an explicit mode,
 * leaving System to the preferences page.
 */
export function TitleBarThemeToggle() {
  const { t } = useTranslation();
  const strip = useTitleBarStrip();
  const { themeMode, setTheme } = useTheme();

  if (!strip.enabled || !strip.themeSlot) return null;

  const isDark =
    themeMode === "dark" || (themeMode === "system" && prefersDark());
  const label = isDark
    ? t("titleBar.switchToLight", "Switch to light mode")
    : t("titleBar.switchToDark", "Switch to dark mode");

  return createPortal(
    <button
      type="button"
      className="title-bar-button"
      aria-label={label}
      title={label}
      data-testid="title-bar-theme-toggle"
      onClick={() => setTheme(isDark ? "light" : "dark")}
    >
      <Icon name={isDark ? "sun" : "moon"} size="1.125rem" />
    </button>,
    strip.themeSlot,
  );
}
