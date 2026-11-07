import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { useToolWorkflow } from "@app/contexts/ToolWorkflowContext";
import { useNavigationState } from "@app/contexts/NavigationContext";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";
import type { ToolId } from "@app/types/toolId";

interface ToolTab {
  id: string;
  label: string;
  toolId: ToolId | null;
}

/**
 * The strip's All tools / Edit / Convert / E-Sign tabs, portalled in from inside
 * the app because the tool workflow exists only below the strip. All tools returns
 * the left panel to the full tool list; the others open that tool. Renders nothing
 * while the strip is off, without calling the workflow hooks, so a tree without
 * the app providers can still render AppLayout.
 */
export function TitleBarToolTabs() {
  const strip = useTitleBarStrip();
  if (!strip.enabled || !strip.toolTabsSlot) return null;
  return <ToolTabs slot={strip.toolTabsSlot} />;
}

function ToolTabs({ slot }: { slot: HTMLElement }) {
  const { t } = useTranslation();
  const { handleToolSelect, handleBackToTools } = useToolWorkflow();
  const { selectedTool } = useNavigationState();

  const tabs: ToolTab[] = [
    { id: "all", label: t("titleBar.allTools", "All tools"), toolId: null },
    {
      id: "edit",
      label: t("titleBar.edit", "Edit"),
      toolId: "pdfTextEditor",
    },
    {
      id: "convert",
      label: t("titleBar.convert", "Convert"),
      toolId: "convert",
    },
    { id: "sign", label: t("titleBar.eSign", "E-Sign"), toolId: "sign" },
  ];

  return createPortal(
    <div
      className="title-bar-tool-tabs"
      role="tablist"
      aria-label={t("titleBar.toolTabs", "Tools")}
    >
      {tabs.map((tab) => {
        const current = tab.toolId
          ? selectedTool === tab.toolId
          : !selectedTool;
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={current}
            className="title-bar-tool-tab"
            onClick={() =>
              tab.toolId ? handleToolSelect(tab.toolId) : handleBackToTools()
            }
          >
            {tab.label}
          </button>
        );
      })}
    </div>,
    slot,
  );
}
