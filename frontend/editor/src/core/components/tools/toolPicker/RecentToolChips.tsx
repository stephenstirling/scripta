import { useTranslation } from "react-i18next";
import { useToolWorkflowData } from "@app/contexts/ToolWorkflowContext";

interface RecentToolChipsProps {
  selectedToolKey: string | null;
  onSelect: (id: string) => void;
}

/** The last few tools used, as one-tap chips above the tool list. Hidden until there is one. */
export function RecentToolChips({
  selectedToolKey,
  onSelect,
}: RecentToolChipsProps) {
  const { t } = useTranslation();
  const { recentTools, toolRegistry } = useToolWorkflowData();

  const chips = recentTools
    .map((id) => ({ id, tool: toolRegistry[id] }))
    .filter(({ tool }) => Boolean(tool));

  if (chips.length === 0) return null;

  return (
    <div className="tool-picker__recent">
      <div className="tool-picker__recent-label">
        {t("toolPanel.recentlyUsed", "Recently used")}
      </div>
      <div className="tool-picker__recent-chips">
        {chips.map(({ id, tool }) => (
          <button
            key={id}
            type="button"
            className="tool-picker__recent-chip"
            data-selected={selectedToolKey === id || undefined}
            onClick={() => onSelect(id)}
          >
            {tool?.name}
          </button>
        ))}
      </div>
    </div>
  );
}
