import { readFileSync } from "fs";
import { join } from "path";
import { fileURLToPath } from "url";
import { describe, expect, it } from "vitest";
import {
  FEATURED_TOOLS,
  SCRIPTA_ADDED_TOOL_IDS,
  toolHueColor,
  type ToolHue,
} from "@app/data/toolHues";
import { isRegularToolId } from "@app/types/toolId";

const themeDir = join(fileURLToPath(import.meta.url), "../../theme");
const primitives = readFileSync(join(themeDir, "primitives.css"), "utf-8");
const colors = readFileSync(join(themeDir, "colors.css"), "utf-8");

function primitive(name: string): string {
  const match = primitives.match(new RegExp(`--${name}:\\s*(#[0-9a-fA-F]{6})`));
  if (!match) throw new Error(`primitive --${name} is not defined`);
  return match[1];
}

function luminance(hex: string): number {
  const channel = (i: number) => {
    const c = parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * channel(0) + 0.7152 * channel(1) + 0.0722 * channel(2);
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

const HUES = [
  ...new Set(
    FEATURED_TOOLS.map((t) => t.hue).filter(
      (h): h is ToolHue => h !== "accent",
    ),
  ),
];

describe("tool hues", () => {
  it("features each tool once, and only tools that exist", () => {
    const ids = FEATURED_TOOLS.map((t) => t.id);
    expect(new Set(ids).size).toBe(ids.length);
    for (const id of ids) expect(isRegularToolId(id)).toBe(true);
  });

  it("tags only featured tools as added by Scripta", () => {
    const ids = new Set(FEATURED_TOOLS.map((t) => t.id));
    for (const id of SCRIPTA_ADDED_TOOL_IDS) expect(ids.has(id)).toBe(true);
  });

  it("colours a tool with its hue token, and the interface blue for accent", () => {
    expect(toolHueColor("convert")).toBe("var(--c-tool-teal)");
    expect(toolHueColor("headerFooter")).toBe("var(--c-accent-text)");
    expect(toolHueColor("rotate")).toBeUndefined();
  });

  it.each(HUES)("defines a %s token for both themes", (hue) => {
    expect(colors).toContain(`--c-tool-${hue}: var(--p-tool-${hue}-on-light);`);
    expect(colors).toContain(`--c-tool-${hue}: var(--p-tool-${hue}-on-dark);`);
  });

  it.each(HUES)("keeps the %s icon at least 3:1 against its panel", (hue) => {
    const darkPanel = primitive("p-acro-d-100");
    const lightPanel = primitive("p-white");
    expect(
      contrast(primitive(`p-tool-${hue}-on-dark`), darkPanel),
    ).toBeGreaterThanOrEqual(3);
    expect(
      contrast(primitive(`p-tool-${hue}-on-light`), lightPanel),
    ).toBeGreaterThanOrEqual(3);
  });
});
