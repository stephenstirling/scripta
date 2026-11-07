import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ActionIcon } from "@app/ui/ActionIcon";
import { Icon, type IconName } from "@app/ui/Icon";
import { Tooltip } from "@app/components/shared/Tooltip";
import { useAnnotation } from "@app/contexts/AnnotationContext";
import { useToolWorkflow } from "@app/contexts/ToolWorkflowContext";
import type { AnnotationToolId } from "@app/components/viewer/viewerTypes";
import "@app/components/viewer/ViewerQuickTools.css";

const SWATCHES = ["#facc15", "#4ade80", "#f472b6", "#60a5fa"];

interface QuickTool {
  id: AnnotationToolId;
  icon: IconName;
  label: string;
  /** Whether the swatches set this tool's colour. */
  usesColor: boolean;
}

/**
 * The viewer's everyday markup tools in one place: select, highlight, draw and
 * text box arm the annotation tool directly, so none of them needs the full
 * Annotate panel; Sign opens the signing tool.
 */
export function ViewerQuickTools() {
  const { t } = useTranslation();
  const { annotationApiRef } = useAnnotation();
  const { handleToolSelect } = useToolWorkflow();
  const [activeId, setActiveId] = useState<AnnotationToolId>("select");
  const [color, setColor] = useState(SWATCHES[0]);

  const tools: QuickTool[] = [
    {
      id: "select",
      icon: "mouse-pointer-2",
      label: t("viewerQuickTools.select", "Select"),
      usesColor: false,
    },
    {
      id: "highlight",
      icon: "highlighter",
      label: t("viewerQuickTools.highlight", "Highlight"),
      usesColor: true,
    },
    {
      id: "ink",
      icon: "pencil",
      label: t("viewerQuickTools.draw", "Draw"),
      usesColor: true,
    },
    {
      id: "text",
      icon: "type",
      label: t("viewerQuickTools.text", "Text box"),
      usesColor: false,
    },
  ];

  const arm = (tool: QuickTool, nextColor = color) => {
    setActiveId(tool.id);
    annotationApiRef.current?.activateAnnotationTool(
      tool.id,
      tool.usesColor ? { color: nextColor } : undefined,
    );
  };

  const pickColor = (swatch: string) => {
    setColor(swatch);
    const active = tools.find((tool) => tool.id === activeId);
    if (active?.usesColor) arm(active, swatch);
  };

  return (
    <div className="viewer-quick-tools" role="group">
      {tools.map((tool) => (
        <Tooltip key={tool.id} content={tool.label} position="bottom">
          <ActionIcon
            variant={activeId === tool.id ? "primary" : "tertiary"}
            className="workbench-bar-action-icon"
            aria-label={tool.label}
            aria-pressed={activeId === tool.id}
            onClick={() => arm(tool)}
          >
            <Icon name={tool.icon} size="1.125rem" />
          </ActionIcon>
        </Tooltip>
      ))}
      <Tooltip content={t("viewerQuickTools.sign", "Sign")} position="bottom">
        <ActionIcon
          variant="tertiary"
          className="workbench-bar-action-icon"
          aria-label={t("viewerQuickTools.sign", "Sign")}
          onClick={() => handleToolSelect("sign")}
        >
          <Icon name="signature" size="1.125rem" />
        </ActionIcon>
      </Tooltip>
      <span className="viewer-quick-tools__rule" />
      {SWATCHES.map((swatch) => (
        <button
          key={swatch}
          type="button"
          className="viewer-quick-tools__swatch"
          style={{ background: swatch }}
          data-selected={swatch === color || undefined}
          aria-label={swatch}
          aria-pressed={swatch === color}
          onClick={() => pickColor(swatch)}
        />
      ))}
    </div>
  );
}
