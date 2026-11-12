import { renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { QuickNavHostActions } from "@app/contexts/QuickNavHostContext";
import {
  isOpenFileShortcut,
  useOpenFileShortcut,
} from "@app/components/layout/useOpenFileShortcut";

const press = (init: KeyboardEventInit) => {
  const event = new KeyboardEvent("keydown", { cancelable: true, ...init });
  window.dispatchEvent(event);
  return event;
};

describe("useOpenFileShortcut", () => {
  it("runs Open from computer on Ctrl+O and stops the browser's own dialog", () => {
    const openFromComputer = vi.fn();
    const actions = { current: { openFromComputer } as QuickNavHostActions };
    renderHook(() => useOpenFileShortcut(actions));

    const event = press({ key: "o", code: "KeyO", ctrlKey: true });

    expect(openFromComputer).toHaveBeenCalledTimes(1);
    expect(event.defaultPrevented).toBe(true);
  });

  it("accepts ⌘O", () => {
    const openFromComputer = vi.fn();
    renderHook(() => useOpenFileShortcut({ current: { openFromComputer } }));

    press({ key: "o", code: "KeyO", metaKey: true });

    expect(openFromComputer).toHaveBeenCalledTimes(1);
  });

  it("leaves the key alone when there is nothing to open", () => {
    renderHook(() => useOpenFileShortcut({ current: {} }));

    const event = press({ key: "o", code: "KeyO", ctrlKey: true });

    expect(event.defaultPrevented).toBe(false);
  });

  it("stops listening when unmounted", () => {
    const openFromComputer = vi.fn();
    const { unmount } = renderHook(() =>
      useOpenFileShortcut({ current: { openFromComputer } }),
    );
    unmount();

    press({ key: "o", code: "KeyO", ctrlKey: true });

    expect(openFromComputer).not.toHaveBeenCalled();
  });
});

describe("isOpenFileShortcut", () => {
  const key = (init: KeyboardEventInit) => new KeyboardEvent("keydown", init);

  it("ignores O without a modifier, with Shift or Alt, and held repeats", () => {
    expect(isOpenFileShortcut(key({ key: "o" }))).toBe(false);
    expect(
      isOpenFileShortcut(key({ key: "O", ctrlKey: true, shiftKey: true })),
    ).toBe(false);
    expect(
      isOpenFileShortcut(key({ key: "o", ctrlKey: true, altKey: true })),
    ).toBe(false);
    expect(
      isOpenFileShortcut(key({ key: "o", ctrlKey: true, repeat: true })),
    ).toBe(false);
  });

  it("matches the O key on layouts where it types another letter", () => {
    expect(
      isOpenFileShortcut(key({ key: "ö", code: "KeyO", ctrlKey: true })),
    ).toBe(true);
  });
});
