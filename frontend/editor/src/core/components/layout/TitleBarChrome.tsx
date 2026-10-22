import { useMemo, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "@app/ui/Icon";
import { useIsPhone } from "@app/hooks/useIsMobile";
import {
  TitleBarStripContext,
  type TitleBarStripSlots,
} from "@app/contexts/TitleBarStripContext";
import { useTitleBarNavigation } from "@app/components/layout/useTitleBarNavigation";
import "@app/components/layout/TitleBarChrome.css";

/**
 * The strip above the app: menu, home and one tab per open document on the top
 * row; the view switcher, search and export actions on the second. It owns only
 * the empty slots and the buttons that need no UI provider. Everything else is
 * portalled in from inside the app, because the file, viewer and navigation
 * state and Mantine all live in providers below this component.
 *
 * The tree is identical whether the strip shows or not, so hiding it on a phone
 * or on a route without app chrome never remounts the app.
 */
export function TitleBarChrome({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const { host, goHome } = useTitleBarNavigation();
  const isPhone = useIsPhone();

  const [menuSlot, setMenuSlot] = useState<HTMLElement | null>(null);
  const [tabsSlot, setTabsSlot] = useState<HTMLElement | null>(null);
  const [toolTabsSlot, setToolTabsSlot] = useState<HTMLElement | null>(null);
  const [themeSlot, setThemeSlot] = useState<HTMLElement | null>(null);
  const [viewsSlot, setViewsSlot] = useState<HTMLElement | null>(null);
  const [searchSlot, setSearchSlot] = useState<HTMLElement | null>(null);
  const [globalsSlot, setGlobalsSlot] = useState<HTMLElement | null>(null);

  const visible = !isPhone && Boolean(host?.appMounted) && !host?.chromeless;

  const slots = useMemo<TitleBarStripSlots>(
    () => ({
      enabled: visible,
      menuSlot,
      tabsSlot,
      toolTabsSlot,
      themeSlot,
      viewsSlot,
      searchSlot,
      globalsSlot,
    }),
    [
      visible,
      menuSlot,
      tabsSlot,
      toolTabsSlot,
      themeSlot,
      viewsSlot,
      searchSlot,
      globalsSlot,
    ],
  );

  const openFromComputer = host?.actions.current?.openFromComputer;

  return (
    <TitleBarStripContext.Provider value={slots}>
      <div className="title-bar-chrome" data-strip={visible || undefined}>
        <header className="title-bar-strip" hidden={!visible}>
          <div className="title-bar-row title-bar-row--tabs">
            <div className="title-bar-slot" ref={setMenuSlot} />
            <button
              type="button"
              className="title-bar-button"
              aria-label={t("titleBar.home", "Home")}
              title={t("titleBar.home", "Home")}
              onClick={goHome}
            >
              <Icon name="house" size="1.125rem" />
            </button>
            <div
              className="title-bar-slot title-bar-slot--tabs"
              ref={setTabsSlot}
            />
            {openFromComputer && (
              <button
                type="button"
                className="title-bar-button title-bar-button--outlined title-bar-add-files"
                onClick={() => openFromComputer()}
              >
                <Icon name="plus" size="0.875rem" />
                <span>{t("fileSidebar.addFiles", "Add files")}</span>
              </button>
            )}
            <div className="title-bar-slot title-bar-slot--theme" ref={setThemeSlot} />
          </div>
          <div className="title-bar-row title-bar-row--toolbar">
            <div className="title-bar-slot" ref={setToolTabsSlot} />
            <div className="title-bar-slot" ref={setViewsSlot} />
            <div
              className="title-bar-slot title-bar-slot--search"
              ref={setSearchSlot}
            />
            <div className="title-bar-slot" ref={setGlobalsSlot} />
          </div>
        </header>
        <div className="title-bar-body">{children}</div>
      </div>
    </TitleBarStripContext.Provider>
  );
}
