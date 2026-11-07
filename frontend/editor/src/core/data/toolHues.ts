import type { ToolId } from "@app/types/toolId";

/** A hue with a `--c-tool-<hue>` token in theme/colors.css; "accent" is the interface blue. */
export type ToolHue =
  | "teal"
  | "pink"
  | "red"
  | "cyan"
  | "lime"
  | "amber"
  | "violet"
  | "green"
  | "indigo"
  | "rose"
  | "orange"
  | "purple"
  | "accent";

/**
 * The tools shown in the tool panel's resting list, in display order, each with the hue
 * of its icon. The hue colours the icon stroke and nothing else; every other tool keeps
 * the neutral icon colour. See docs/design/UI_DESIGN.md.
 */
export const FEATURED_TOOLS: ReadonlyArray<{ id: ToolId; hue: ToolHue }> = [
  { id: "convert", hue: "teal" },
  { id: "pdfTextEditor", hue: "pink" },
  { id: "split", hue: "red" },
  { id: "merge", hue: "cyan" },
  { id: "reorganizePages", hue: "lime" },
  { id: "annotate", hue: "amber" },
  { id: "sign", hue: "violet" },
  { id: "ocr", hue: "green" },
  { id: "addPassword", hue: "indigo" },
  { id: "redact", hue: "rose" },
  { id: "compress", hue: "orange" },
  { id: "formFill", hue: "purple" },
  { id: "headerFooter", hue: "accent" },
  { id: "batesNumbering", hue: "accent" },
];

/** Tools Scripta added on top of the upstream set; the panel tags them "New". */
export const SCRIPTA_ADDED_TOOL_IDS: ReadonlySet<ToolId> = new Set<ToolId>([
  "headerFooter",
  "batesNumbering",
]);

const HUE_BY_TOOL = new Map<ToolId, ToolHue>(
  FEATURED_TOOLS.map(({ id, hue }) => [id, hue]),
);

/** The CSS colour for a tool's icon, or undefined for a tool without a hue. */
export function toolHueColor(id: ToolId): string | undefined {
  const hue = HUE_BY_TOOL.get(id);
  if (!hue) return undefined;
  return hue === "accent" ? "var(--c-accent-text)" : `var(--c-tool-${hue})`;
}
