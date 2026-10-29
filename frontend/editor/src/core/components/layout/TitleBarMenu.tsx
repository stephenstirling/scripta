import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Menu } from "@mantine/core";
import { Icon } from "@app/ui/Icon";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";
import { useTitleBarNavigation } from "@app/components/layout/useTitleBarNavigation";
import { DOCS_PATH, HAS_DOCS } from "@app/routes/docsRoute";

/**
 * The strip's Menu button. It is portalled into the strip from inside the app
 * because Mantine's Menu needs the UI provider, which sits below the strip.
 * Renders nothing while the strip is off, without calling those hooks.
 */
export function TitleBarMenu() {
  const strip = useTitleBarStrip();
  if (!strip.enabled || !strip.menuSlot) return null;
  return <MenuButton slot={strip.menuSlot} />;
}

function MenuButton({ slot }: { slot: HTMLElement }) {
  const { t } = useTranslation();
  const { host, guarded, navigate, showLibrary, openSettings } =
    useTitleBarNavigation();

  return createPortal(
    <Menu position="bottom-start" width="14rem" withinPortal>
      <Menu.Target>
        <button
          type="button"
          className="title-bar-button title-bar-button--labelled"
          data-testid="title-bar-menu"
        >
          <Icon name="menu" size="1rem" />
          <span>{t("titleBar.menu", "Menu")}</span>
        </button>
      </Menu.Target>
      <Menu.Dropdown>
        {host?.hasOpenFromComputer && (
          <Menu.Item
            leftSection={<Icon name="file-up" size="1rem" />}
            onClick={() => host.actions.current?.openFromComputer?.()}
          >
            {t("fileSidebar.openFromComputer", "Open from computer")}
          </Menu.Item>
        )}
        <Menu.Item
          leftSection={<Icon name="folder" size="1rem" />}
          onClick={showLibrary}
        >
          {t("fileSidebar.myFiles", "File library")}
        </Menu.Item>
        <Menu.Item
          leftSection={<Icon name="settings" size="1rem" />}
          onClick={openSettings}
        >
          {t("settings.title", "Settings")}
        </Menu.Item>
        {HAS_DOCS && (
          <Menu.Item
            leftSection={<Icon name="book-open" size="1rem" />}
            onClick={() => guarded(() => navigate(DOCS_PATH))}
          >
            {t("quickNav.docs", "Documentation")}
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>,
    slot,
  );
}
