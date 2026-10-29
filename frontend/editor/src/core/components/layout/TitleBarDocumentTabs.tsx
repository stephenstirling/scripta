import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Icon } from "@app/ui/Icon";
import { useAllFiles, useFileActions } from "@app/contexts/FileContext";
import { useViewer } from "@app/contexts/ViewerContext";
import { useNavigationActions } from "@app/contexts/NavigationContext";
import { useTitleBarStrip } from "@app/contexts/TitleBarStripContext";
import { PrivateContent } from "@app/components/shared/PrivateContent";
import { truncateCenter } from "@app/utils/textUtils";
import type { FileId } from "@app/types/file";

/**
 * One tab per open document, portalled into the title-bar strip. It lives here
 * rather than in the strip because the file and viewer state exist only inside
 * the app providers; the strip sits above them. Renders nothing while the strip
 * is off, and never calls those providers' hooks then, so a tree without them
 * (a unit test, a phone) can still render AppLayout.
 */
export function TitleBarDocumentTabs() {
  const strip = useTitleBarStrip();
  if (!strip.enabled || !strip.tabsSlot) return null;
  return <DocumentTabs slot={strip.tabsSlot} />;
}

function DocumentTabs({ slot }: { slot: HTMLElement }) {
  const { t } = useTranslation();
  const { fileStubs } = useAllFiles();
  const { actions: fileActions } = useFileActions();
  const { activeFileId, setActiveFileId } = useViewer();
  const { actions } = useNavigationActions();

  if (fileStubs.length === 0) return null;

  const currentId = activeFileId ?? fileStubs[0].id;

  const close = async (id: FileId) => {
    if (id === currentId) {
      const next = fileStubs.find((stub) => stub.id !== id);
      if (next) setActiveFileId(next.id);
    }
    await fileActions.removeFiles([id], false);
  };

  return createPortal(
    <div
      className="title-bar-tabs"
      role="tablist"
      aria-label={t("titleBar.documents", "Open documents")}
    >
      {fileStubs.map((stub) => {
        const selected = stub.id === currentId;
        return (
          <div
            key={stub.id}
            className="title-bar-tab"
            data-active={selected || undefined}
          >
            <button
              type="button"
              role="tab"
              aria-selected={selected}
              className="title-bar-tab__label"
              title={stub.name}
              onClick={() => {
                setActiveFileId(stub.id);
                actions.setWorkbench("viewer");
              }}
            >
              <Icon name="file" size="0.875rem" />
              <PrivateContent>
                <span>{truncateCenter(stub.name, 28)}</span>
              </PrivateContent>
              {stub.toolHistory?.some((op) => op.toolId === "ocr") && (
                <span
                  className="title-bar-tab__status"
                  role="img"
                  aria-label={t("titleBar.textRecognised", "Text recognised")}
                  title={t("titleBar.textRecognised", "Text recognised")}
                />
              )}
            </button>
            <button
              type="button"
              className="title-bar-tab__close"
              aria-label={t("titleBar.closeDocument", "Close {{name}}", {
                name: stub.name,
              })}
              onClick={() => void close(stub.id)}
            >
              <Icon name="x" size="0.75rem" />
            </button>
          </div>
        );
      })}
    </div>,
    slot,
  );
}
