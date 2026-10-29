# Scripta UI design

The target look for the editor. Read this before changing layout, colour, icons or spacing in
`frontend/editor`. It sits on top of the two rules documents it depends on:
[`theme/README.md`](../../frontend/editor/src/core/theme/README.md) (colour tokens) and
[`icons/README.md`](../../frontend/editor/src/core/icons/README.md) (icon registry).

Scripta follows the conventions of a desktop PDF editor: a tab strip across the top, a tool list
on the left, the document on a grey canvas, floating tools beside it. The interactive mockup
(dark, light, home screen and colour sheet) is at
<https://claude.ai/artifact/8BUYrLNCnmLXz1NB5ddxQs>. It is a private link, so ask the owner for access.

## What not to copy

Layout, density and interaction patterns are fair game. Do not use another product's name, logo,
icon artwork, typeface or marketing copy. Specifically:

- Scripta's own tool names and its own icon drawings (Lucide plus `svg/stirling/`).
- The top-strip button is "Add files", not "Create": Scripta does not author PDFs from scratch.
- No upgrade or promotional block at the foot of the tool panel.

## Layout

Light and dark are both first-class. The sizes below are shared tokens in `theme/dimensions.css`.

| Region | Spec |
| --- | --- |
| Top row (`--chrome-tabstrip-h`, 2.5rem) | Menu, Home, one tab per open document (file icon, name, a dot when text is recognised, close), outlined "Add files", and the theme toggle at the far right. |
| Second row (`--chrome-toolbar-h`, 2.75rem) | Tool tabs All tools, Edit, Convert, E-Sign; the active tab is accent text with a 2px underline. With a document open: the view switcher (Viewer, Page editor, File library). On the right: a 22rem search field, then print, download and close. |
| Tool panel (left) | 296px wide. A 56px header with "All tools" and a close button, then a flat list of tools, then a "View more" link. |
| Canvas | `--c-canvas` with the white page on it. Floating quick-tools bar at the top centre (select, highlight, draw, text box, sign, four highlight colours, more). Vertical page-tools bar on the right edge. Page and zoom bar at the bottom centre. |
| PDF Library sidebar | Stays, as a collapsible panel. It does not replace or get replaced by the tool panel. |
| Home screen | Greeting, three action cards (open from computer, scan from a scanner, make a scan searchable), then recent files. |
| Phones | The strip is hidden and the existing single-column layout applies. |

## The tool list

One flat list, no section headings. Each row:

- Height 42px, side padding 24px, 16px between icon and label, label 14px.
- Icon 24px, outlined (stroke 1.6, round caps and joins), drawn in the tool's hue. No filled tile or
  background behind it.
- Tools Scripta added carry a small "New" tag in the interface blue.

| Tool | Hue | Tool | Hue |
| --- | --- | --- | --- |
| Convert files | teal | Scan & OCR | green |
| PDF Text Editor | pink | Protect | indigo |
| Create PDF | red | Redact | rose |
| Merge files | cyan | Compress | orange |
| Organize pages | lime | Forms | purple |
| Annotate | amber | Header & Footer (new) | interface blue |
| Sign | violet | Bates numbering (new) | interface blue |

Hues are used only for these icons. Do not use them for buttons, text, borders or fills.

## Colour

All values are tokens. Literal colours live only in `theme/primitives.css`; components use `--c-*`
(see the theme README). Surfaces and text, already implemented:

| Role | Token | Dark | Light |
| --- | --- | --- | --- |
| Canvas behind the page | `--c-canvas` | `#171717` | `#e1e1e1` |
| App background | `--c-bg` | `#1e1e1e` | `#f5f5f5` |
| Top bars | `--c-chrome` | `#252525` | `#f5f5f5` |
| Tool panel | `--c-bg-raised` | `#252525` | `#ffffff` |
| Cards, fields | `--c-surface` | `#2a2a2a` | `#ffffff` |
| Hairlines between regions | `--c-chrome-border` | `#444444` | `#d5d5d5` |
| Text | `--c-text` | `#e6e6e6` | `#2c2c2c` |
| Secondary text | `--c-text-muted` | `#b8b8b8` | `#4b4b4b` |
| Quiet text, hints | `--c-text-subtle` | `#9c9c9c` | `#6e6e6e` |
| Accent (buttons, selection) | `--c-primary` | `#1473e6` | `#1473e6` |
| Accent as text | `--c-accent-text` | `#4b9cf5` | `#0b5cbc` |

Tool hues, **not yet implemented**: add them as `--c-tool-<hue>` in `colors.css`, mapped from new
`--p-tool-<hue>` primitives (light values are deepened so each icon stays legible on white):

| Hue | Dark | Light | Hue | Dark | Light |
| --- | --- | --- | --- | --- | --- |
| teal | `#3fd0c9` | `#0f7f7a` | green | `#4cd48a` | `#17803f` |
| pink | `#f06bb0` | `#c42a7d` | indigo | `#8b95ff` | `#4450c8` |
| red | `#ff6b6b` | `#c92d2d` | rose | `#ff5c8a` | `#c0284f` |
| cyan | `#4fc3f7` | `#0b78a8` | orange | `#ff8a4c` | `#c45a00` |
| lime | `#a4de4a` | `#4d7f0a` | purple | `#c77dff` | `#8a2fd0` |
| amber | `#ffc43d` | `#9c6600` | interface blue | `#4b9cf5` | `#0b5cbc` |
| violet | `#b28bff` | `#6d3fe6` | | | |

Every hue measures at least 4.4:1 against its panel (`#252525` dark, `#ffffff` light). Keep it
that way: the floor for an icon is 3:1, and `theme-lint` already enforces a legibility floor for
status tones, so extend it to cover these.

## Building it

- Colour the icon, not a wrapper: `ToolIcon` takes a `color` prop, and any `<Icon>` accepts
  `style={{ color: "var(--c-tool-teal)" }}`. Icons still come from the registry in `icons/icons.ts`.
- Keep the hue on the tool's registry entry, so the tool panel, search results and the home
  screen all colour a tool the same way.
- New and changed strings go in `public/locales/en-US/translation.toml` only.
- Run `task frontend:check` after changing frontend files.
