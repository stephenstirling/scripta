import { createContext, useContext } from "react";

/** Optional in-window title-bar strip. */
export interface TitleBarStripSlots {
  enabled: boolean;
  /** The app menu, rendered from inside the app because it needs the UI provider. */
  menuSlot: HTMLElement | null;
  /** One tab per open document, rendered from inside the app where the file state lives. */
  tabsSlot: HTMLElement | null;
  viewsSlot: HTMLElement | null;
  searchSlot: HTMLElement | null;
  globalsSlot: HTMLElement | null;
}

const DEFAULT: TitleBarStripSlots = {
  enabled: false,
  menuSlot: null,
  tabsSlot: null,
  viewsSlot: null,
  searchSlot: null,
  globalsSlot: null,
};

export const TitleBarStripContext = createContext<TitleBarStripSlots>(DEFAULT);

export function useTitleBarStrip(): TitleBarStripSlots {
  return useContext(TitleBarStripContext);
}
