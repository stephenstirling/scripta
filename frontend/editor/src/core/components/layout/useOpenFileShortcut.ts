import { useEffect, type RefObject } from "react";
import { isMacLike } from "@app/utils/hotkeys";
import type { QuickNavHostActions } from "@app/contexts/QuickNavHostContext";

/** Ctrl+O, or ⌘O on a Mac, without Shift or Alt. */
export function isOpenFileShortcut(event: KeyboardEvent): boolean {
  if (event.altKey || event.shiftKey || event.repeat) return false;
  if (!(event.ctrlKey || event.metaKey)) return false;
  return event.key.toLowerCase() === "o" || event.code === "KeyO";
}

/** The shortcut as the menus show it on this platform. */
export function openFileShortcutLabel(): string {
  return isMacLike() ? "⌘O" : "Ctrl+O";
}

/**
 * Makes Ctrl+O open files, as in a desktop PDF editor, by running the same
 * action as "Open from computer". The browser's own Open dialog is suppressed
 * only when there is an action to run, so pages without one behave as before.
 */
export function useOpenFileShortcut(
  actions: RefObject<QuickNavHostActions> | undefined,
): void {
  useEffect(() => {
    if (!actions) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (!isOpenFileShortcut(event)) return;
      const open = actions.current?.openFromComputer;
      if (!open) return;
      event.preventDefault();
      open();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [actions]);
}
