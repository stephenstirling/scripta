import { describe, expect, it } from "vitest";
import type { ReactElement } from "react";
import type { TFunction } from "i18next";
import {
  createPreferencesNavSection,
  extendPreferences,
} from "@app/components/shared/config/configNavSections";
import PreferencesSection, {
  type PreferencesSectionProps,
} from "@app/components/shared/config/configSections/preferences/PreferencesSection";

const t = ((key: string, fallback?: string) =>
  fallback ?? key) as unknown as TFunction<"translation", undefined>;

function page(sections: ReturnType<typeof extendPreferences>) {
  return sections[0].items[0]
    .component as ReactElement<PreferencesSectionProps>;
}

describe("extendPreferences", () => {
  it("keeps what earlier layers passed when a later one adds its own", () => {
    const base = [
      createPreferencesNavSection(t, { editorDefaultsSlot: "extra" }),
    ];
    const withAccount = extendPreferences(base, { accountSlot: "account" });
    const withUpdates = extendPreferences(withAccount, {
      hideUpdateSection: true,
    });

    const props = page(withUpdates).props;
    expect(page(withUpdates).type).toBe(PreferencesSection);
    expect(props.editorDefaultsSlot).toBe("extra");
    expect(props.accountSlot).toBe("account");
    expect(props.hideUpdateSection).toBe(true);
  });

  it("keeps a wrapper page when a later layer adds props", () => {
    const Wrapper = (_props: PreferencesSectionProps) => null;
    const wrapped = extendPreferences(
      [createPreferencesNavSection(t, { accountSlot: "account" })],
      {},
      Wrapper,
    );
    const extended = extendPreferences(wrapped, {
      editorDefaultsSlot: "extra",
    });

    expect(page(extended).type).toBe(Wrapper);
    expect(page(extended).props.accountSlot).toBe("account");
    expect(page(extended).props.editorDefaultsSlot).toBe("extra");
  });

  it("gives every Preferences page the same props and keeps which page it draws", () => {
    const sections = extendPreferences([createPreferencesNavSection(t)], {
      hideUpdateSection: true,
    });

    const pages = sections[0].items.map((item) => {
      const element = item.component as ReactElement<PreferencesSectionProps>;
      return [item.key, element.props.page, element.props.hideUpdateSection];
    });
    expect(pages).toEqual([
      ["general", "general", true],
      ["downloads", "downloads", true],
      ["updates", "updates", true],
      ["hotkeys", "hotkeys", true],
    ]);
  });

  it("leaves every other row alone", () => {
    const other = {
      key: "about" as const,
      label: "About",
      icon: "settings" as const,
    };
    const sections = extendPreferences(
      [{ title: "About", items: [{ ...other, component: null }] }],
      {
        accountSlot: "account",
      },
    );
    expect(sections[0].items[0].component).toBeNull();
  });
});
