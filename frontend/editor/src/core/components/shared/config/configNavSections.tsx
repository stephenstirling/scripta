import { isValidElement, type ComponentType } from "react";
import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import PreferencesSection, {
  type PreferencesSectionProps,
} from "@app/components/shared/config/configSections/preferences/PreferencesSection";
import AboutSection from "@app/components/shared/config/configSections/AboutSection";
import type {
  ConfigNavItem,
  ConfigNavSection,
} from "@app/components/shared/config/types";

// Re-exported for the many existing importers; the definitions live in
// config/types so type-only consumers don't pull the section tree in.
export type { ConfigNavItem, ConfigNavSection };

export interface ConfigColors {
  navBg: string;
  sectionTitle: string;
  navItem: string;
  navItemActive: string;
  navItemActiveBg: string;
  contentBg: string;
  headerBorder: string;
}

/** Nav key of the first Preferences page; old links to "general" still land here. */
export const PREFERENCES_ITEM_KEY = "general";

/** Nav keys of every Preferences page, which share one set of page props. */
export const PREFERENCES_PAGE_KEYS: ReadonlySet<string> = new Set([
  PREFERENCES_ITEM_KEY,
  "downloads",
  "updates",
  "hotkeys",
]);

/**
 * The Preferences group: one nav row per page. A plain builder rather than a
 * hook so the SaaS nav, which is assembled outside a component, renders the
 * same pages.
 */
export function createPreferencesNavSection(
  t: TFunction<"translation", undefined>,
  props: PreferencesSectionProps = {},
): ConfigNavSection {
  return {
    id: "preferences",
    title: t("settings.preferences.title", "Preferences"),
    items: [
      {
        key: PREFERENCES_ITEM_KEY,
        label: t("settings.general.title", "General"),
        description: t(
          "settings.general.pageDescription",
          "How Scripta looks and what the editor opens with.",
        ),
        icon: "sliders-horizontal",
        component: <PreferencesSection {...props} page="general" />,
      },
      {
        key: "downloads",
        label: t("settings.preferences.downloads", "Downloads"),
        description: t(
          "settings.preferences.downloadsDescription",
          "What happens to ZIP files that a tool gives back.",
        ),
        icon: "download",
        component: <PreferencesSection {...props} page="downloads" />,
      },
      {
        key: "updates",
        label: t("settings.general.updates.title", "Software Updates"),
        description: t(
          "settings.general.updates.description",
          "Check for updates and view version information",
        ),
        icon: "refresh-cw",
        component: <PreferencesSection {...props} page="updates" />,
      },
      {
        key: "hotkeys",
        label: t("settings.hotkeys.title", "Keyboard Shortcuts"),
        description: t(
          "settings.hotkeys.description",
          'Customize keyboard shortcuts for quick tool access. Click "Change shortcut" and press a new key combination. Press Esc to cancel.',
        ),
        icon: "keyboard",
        component: <PreferencesSection {...props} page="hotkeys" />,
      },
    ],
  };
}

/**
 * Lays `extra` over the Preferences page's props so layers compose, not clobber.
 * `Page` swaps in a wrapper for extras that need hooks; omitted, the page is kept.
 */
export function extendPreferences(
  sections: ConfigNavSection[],
  extra: PreferencesSectionProps,
  Page?: ComponentType<PreferencesSectionProps>,
): ConfigNavSection[] {
  return sections.map((section) => ({
    ...section,
    items: section.items.map((item) => {
      if (!PREFERENCES_PAGE_KEYS.has(item.key)) return item;
      const current = isValidElement<PreferencesSectionProps>(item.component)
        ? item.component
        : null;
      const Component =
        Page ??
        (current?.type as ComponentType<PreferencesSectionProps> | undefined) ??
        PreferencesSection;
      return {
        ...item,
        component: <Component {...current?.props} {...extra} />,
      };
    }),
  }));
}

export const useConfigNavSections = (
  _isAdmin: boolean = false,
  _runningEE: boolean = false,
  _loginEnabled: boolean = false,
  onRequestClose: () => void = () => {},
  _showSettingsWhenNoLogin: boolean = true,
): ConfigNavSection[] => {
  const { t } = useTranslation();

  const sections: ConfigNavSection[] = [
    createPreferencesNavSection(t),
    // Reference material: read once and rarely revisited, so it is one page
    // rather than four rows you have to open in turn.
    {
      id: "about",
      title: t("settings.about.title", "About"),
      items: [
        {
          key: "about",
          label: t("settings.about.title", "About"),
          description: t(
            "settings.about.description",
            "Tours, legal documents and the licences of everything bundled with this build.",
          ),
          icon: "circle-question-mark",
          component: (
            <AboutSection isAdmin={_isAdmin} onRequestClose={onRequestClose} />
          ),
        },
      ],
    },
  ];

  return sections;
};
