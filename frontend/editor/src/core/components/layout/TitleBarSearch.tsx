import { createPortal } from "react-dom";
import SuperSearch from "@app/components/shared/superSearch/SuperSearch";
import { useEditorSearchScopes } from "@app/hooks/useSuperSearch";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";

/**
 * Portals the one Super Search instance into the title-bar strip. It is mounted
 * from AppLayout, not the workbench bar, so the search keeps its query across
 * views. Renders nothing while the strip is off.
 */
export function TitleBarSearch() {
  const strip = useTitleBarStrip();
  const scopes = useEditorSearchScopes();
  if (!strip.enabled || !strip.searchSlot) return null;
  return createPortal(<SuperSearch scopes={scopes} />, strip.searchSlot);
}
