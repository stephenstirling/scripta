import { type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { SettingsCard } from "@app/components/shared/config/SettingsCard";
import { useAppConfig } from "@app/contexts/AppConfigContext";
import { AppearanceCard } from "@app/components/shared/config/configSections/preferences/AppearanceCard";
import { EditorDefaultsCard } from "@app/components/shared/config/configSections/preferences/EditorDefaultsCard";
import { DownloadsCard } from "@app/components/shared/config/configSections/preferences/DownloadsCard";
import { SoftwareUpdatesCard } from "@app/components/shared/config/configSections/preferences/SoftwareUpdatesCard";
import { HotkeysCard } from "@app/components/shared/config/configSections/preferences/HotkeysCard";
import type {
  DesktopInstall,
  DesktopUpdateModeControl,
} from "@app/components/shared/config/configSections/preferences/preferencesCardProps";
import "@app/components/shared/config/SettingsRow.css";
import "@app/components/shared/config/configSections/preferences/PreferencesSection.css";

/** The personal settings pages, each its own row in the settings nav. */
export type PreferencesPage = "general" | "downloads" | "updates" | "hotkeys";

export interface PreferencesSectionProps {
  /** Which page to draw; the nav passes it per row. */
  page?: PreferencesPage;
  /** SaaS build and managed-disabled desktop: no update UI, and no update call. */
  hideUpdateSection?: boolean;
  desktopInstall?: DesktopInstall;
  desktopUpdateMode?: DesktopUpdateModeControl;
  /**
   * The Account and Two-factor cards. Core has no accounts, so the flavors that
   * do pass them in (AccountCards) rather than core reaching into proprietary.
   * The slot supplies its own `<section>` wrappers and anchors.
   */
  accountSlot?: ReactNode;
  /** Extra rows under Editor defaults: login landing, desktop file defaults. */
  editorDefaultsSlot?: ReactNode;
}

/**
 * Everything you set for yourself. None of it saves through the admin API, so
 * no page has a draft or a save footer: every control writes as you touch it.
 */
export default function PreferencesSection({
  page = "general",
  hideUpdateSection = false,
  desktopInstall,
  desktopUpdateMode,
  accountSlot,
  editorDefaultsSlot,
}: PreferencesSectionProps) {
  const { t } = useTranslation();
  const { config } = useAppConfig();

  // A version to report, or the desktop updater. Not mounting the card is what
  // skips the update check.
  const showUpdates =
    !hideUpdateSection && Boolean(config?.appVersion || desktopInstall);

  return (
    <div className="settings-section-container">
      <div className="preferences-section">
        {page === "general" && (
          <>
            {accountSlot}
            <SettingsCard
              id="appearance"
              title={t("settings.preferences.appearance", "Appearance")}
            >
              <AppearanceCard />
            </SettingsCard>
            <SettingsCard
              id="editorDefaults"
              title={t(
                "settings.preferences.editorDefaults",
                "Editor defaults",
              )}
            >
              <EditorDefaultsCard />
              {editorDefaultsSlot}
            </SettingsCard>
          </>
        )}

        {page === "downloads" && <DownloadsCard />}

        {page === "updates" &&
          (showUpdates ? (
            <SoftwareUpdatesCard
              desktopInstall={desktopInstall}
              desktopUpdateMode={desktopUpdateMode}
            />
          ) : (
            <p className="preferences-section__empty">
              {t(
                "settings.general.updates.unavailable",
                "This copy of Scripta has no version to check. Updates come from the app you installed it with.",
              )}
            </p>
          ))}

        {page === "hotkeys" && (
          <div id="hotkeys" className="preferences-section__stack">
            <HotkeysCard />
          </div>
        )}
      </div>
    </div>
  );
}
