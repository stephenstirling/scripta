import { useTranslation } from "react-i18next";
import { SegmentedControl } from "@app/ui/SegmentedControl";
import { useTheme } from "@app/components/shared/ThemeProvider";
import LanguageSelector from "@app/components/shared/LanguageSelector";
import { SettingsRow } from "@app/components/shared/config/SettingsRow";

/** How the app looks: theme and display language. Both write through at once. */
export function AppearanceCard() {
  const { t } = useTranslation();
  const { setTheme, themeMode } = useTheme();

  return (
    <div className="settings-group">
      <SettingsRow
        id="setting-theme"
        label={t("settings.general.theme", "Theme")}
        description={t(
          "settings.general.themeDescription",
          "Choose light, dark, or follow your system so it switches automatically.",
        )}
      >
        <SegmentedControl
          value={themeMode}
          onChange={(val) => setTheme(val)}
          options={[
            {
              label: t("settings.general.themeLight", "Light"),
              value: "light",
            },
            { label: t("settings.general.themeDark", "Dark"), value: "dark" },
            {
              label: t("settings.general.themeSystem", "System"),
              value: "system",
            },
          ]}
        />
      </SettingsRow>
      <SettingsRow
        id="setting-language"
        label={t("settings.general.language", "Language")}
        description={t(
          "settings.general.languageDescription",
          "Choose the display language",
        )}
      >
        <LanguageSelector position="bottom-end" offset={6} />
      </SettingsRow>
    </div>
  );
}
