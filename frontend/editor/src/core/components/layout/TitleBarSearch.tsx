import { createPortal } from "react-dom";
import SuperSearch from "@app/components/shared/superSearch/SuperSearch";
import { useEditorSearchScopes } from "@app/hooks/useSuperSearch";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";

/**
 * Portals the one Super Search instance into the title-bar strip. It is mounted
 * from AppLayout, not the workbench bar, so the search keeps its query across
 * views. Renders nothing while the strip is off, without calling the search
 * hooks, so a tree without the app providers can still render AppLayout.
 */
export function TitleBarSearch() {
  const strip = useTitleBarStrip();
  if (!strip.enabled || !strip.searchSlot) return null;
  return <SearchPortal slot={strip.searchSlot} />;
}

function SearchPortal({ slot }: { slot: HTMLElement }) {
  const scopes = useEditorSearchScopes();
  return createPortal(<SuperSearch scopes={scopes} />, slot);
}
