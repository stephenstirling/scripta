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

- Scripta's own tool names and its own icon drawings (Lucide plus `svg/scripta/`).
- The top-strip button is "Add files", not "Create": Scripta does not author PDFs from scratch.
- No upgrade or promotional block at the foot of the tool panel.

## Layout

Light and dark are both first-class. Sizes use the tokens named below in `theme/dimensions.css`; add any your branch lacks.

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

The panel's resting state is one flat list of the featured tools, with a "View more" link that opens
the full categorised list (and a close button to come back). Pinned favourites come first. Each row:

- Height 42px, 24px side padding, 16px between icon and label, label 14px in the main text colour.
- Icon 24px, outlined, in the tool's hue (the registry's standard stroke). No filled tile or
  background behind it.
- Tools Scripta added carry a small "New" tag in the interface blue.

The featured tools, in order, by tool ID and the name the registry shows. They live in
`data/toolHues.ts`, which is the one place to change them:

| Tool | Hue | Tool | Hue |
| --- | --- | --- | --- |
| Convert (`convert`) | teal | OCR / Cleanup scans (`ocr`) | green |
| PDF Text Editor (`pdfTextEditor`) | pink | Add Password (`addPassword`) | indigo |
| Split (`split`) | red | Redact (`redact`) | rose |
| Merge (`merge`) | cyan | Compress (`compress`) | orange |
| Reorganize Pages (`reorganizePages`) | lime | Form Editor (`formFill`) | purple |
| Annotate (`annotate`) | amber | Header & Footer, new (`headerFooter`) | interface blue |
| Sign (`sign`) | violet | Bates Numbering, new (`batesNumbering`) | interface blue |

Hues are used only for these icons, wherever the tool appears (resting list, full list, search
results). Every other tool keeps the neutral icon colour. Do not use a hue for buttons, text,
borders or fills.

## Colour

All values are tokens. Literal colours live only in `theme/primitives.css`; components use `--c-*`
(see the theme README). Surfaces and text: use these tokens and values, and add any your branch lacks to
`colors.css`, with the literal in `primitives.css`:

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

Tool hues are `--c-tool-<hue>` in `colors.css`, mapped from `--p-tool-<hue>-on-dark` and
`--p-tool-<hue>-on-light` primitives (light values are deepened so each icon stays legible on white):

| Hue | Dark | Light | Hue | Dark | Light |
| --- | --- | --- | --- | --- | --- |
| teal | `#3fd0c9` | `#0f7f7a` | green | `#4cd48a` | `#17803f` |
| pink | `#f06bb0` | `#c42a7d` | indigo | `#8b95ff` | `#4450c8` |
| red | `#ff6b6b` | `#c92d2d` | rose | `#ff5c8a` | `#c0284f` |
| cyan | `#4fc3f7` | `#0b78a8` | orange | `#ff8a4c` | `#c45a00` |
| lime | `#a4de4a` | `#4d7f0a` | purple | `#c77dff` | `#8a2fd0` |
| amber | `#ffc43d` | `#9c6600` | interface blue | `#4b9cf5` | `#0b5cbc` |
| violet | `#b28bff` | `#6d3fe6` | | | |

Every hue measures at least 4.4:1 against its panel (`#252525` dark, `#ffffff` light). The floor for
an icon is 3:1, and `data/toolHues.test.ts` fails if a hue drops below it or loses a token in either
theme.

## Building it

- The row colours its icon through `toolHueColor(id)` in `ToolButton`; the roomy row is
  `ToolButton`'s `roomy` prop. Icons still come from the registry in `icons/icons.ts`.
- Colour the icon, not a wrapper: `ToolIcon` takes a `color` prop, and any `<Icon>` accepts
  `style={{ color: "var(--c-tool-teal)" }}`.
- New and changed strings go in `public/locales/en-US/translation.toml` only.
- Run `task frontend:check` after changing frontend files.
