import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Icon, type IconName } from "@app/ui/Icon";
import { useFilesModalContext } from "@app/contexts/FilesModalContext";
import { useFilesPage } from "@app/contexts/FilesPageContext";
import { useFolders } from "@app/contexts/FolderContext";
import { useFileActions } from "@app/contexts/FileContext";
import { useToolWorkflow } from "@app/contexts/ToolWorkflowContext";
import { useAppConfig } from "@app/contexts/AppConfigContext";
import { useIsMobile } from "@app/hooks/useIsMobile";
import { folderKind } from "@app/types/folder";
import { CreateProcessingFolderButton } from "@app/components/policies/CreateProcessingFolderButton";
import ScanModal from "@app/components/shared/ScanModal";
import "@app/components/shared/LandingHome.css";

const RECENT_COUNT = 5;

interface LandingHomeProps {
  onUploadClick: () => void;
  onMobileUploadClick: () => void;
}

interface HomeAction {
  id: string;
  icon: IconName;
  title: string;
  hint: string;
  onClick: () => void;
}

function greetingKey(hour: number): "morning" | "afternoon" | "evening" {
  if (hour < 12) return "morning";
  if (hour < 18) return "afternoon";
  return "evening";
}

function formatWhen(timestamp: number, locale: string): string {
  return new Date(timestamp).toLocaleDateString(locale, {
    day: "numeric",
    month: "short",
  });
}

/**
 * The editor's empty state: what to do next (open, scan, import, make
 * searchable) and the files most recently worked on.
 */
export function LandingHome({
  onUploadClick,
  onMobileUploadClick,
}: LandingHomeProps) {
  const { t, i18n } = useTranslation();
  const { openFilesModal } = useFilesModalContext();
  const { allFiles, loading } = useFilesPage();
  const folders = useFolders();
  const { actions: fileActions } = useFileActions();
  const { handleToolSelect } = useToolWorkflow();
  const { config } = useAppConfig();
  const isMobile = useIsMobile();
  const [scanOpen, setScanOpen] = useState(false);

  // Mounted directories are listed on demand, so their files may not be cached yet.
  const hasMountedFolders = folders.folders.some(
    (folder) => folderKind(folder) === "local",
  );
  const libraryEmpty =
    !loading && !folders.loading && allFiles.length === 0 && !hasMountedFolders;

  const recent = useMemo(
    () =>
      allFiles
        .filter((file) => file.isLeaf)
        .sort((a, b) => b.lastModified - a.lastModified)
        .slice(0, RECENT_COUNT),
    [allFiles],
  );

  const actions: HomeAction[] = [
    {
      id: "open",
      icon: "plus",
      title: t("landing.home.open", "Open from computer"),
      hint: t("landing.home.openHint", "PDF, images or Office files"),
      onClick: () => (libraryEmpty ? onUploadClick() : openFilesModal()),
    },
    {
      id: "scan",
      icon: "scan",
      title: t("landing.home.scan", "Scan from a scanner"),
      hint: t("landing.home.scanHint", "Network scanners, flatbed or feeder"),
      onClick: () => setScanOpen(true),
    },
    ...(config?.enableMobileScanner && !isMobile
      ? [
          {
            id: "phone",
            icon: "qr-code" as IconName,
            title: t("landing.home.phone", "Import from phone"),
            hint: t("landing.home.phoneHint", "Scan a QR code, send a photo"),
            onClick: onMobileUploadClick,
          },
        ]
      : []),
    {
      id: "ocr",
      icon: "scan-text",
      title: t("landing.home.ocr", "Make a scan searchable"),
      hint: t("landing.home.ocrHint", "OCR, with a Word copy if you want"),
      onClick: () => handleToolSelect("ocr"),
    },
  ];

  const greeting = {
    morning: t("landing.home.goodMorning", "Good morning"),
    afternoon: t("landing.home.goodAfternoon", "Good afternoon"),
    evening: t("landing.home.goodEvening", "Good evening"),
  }[greetingKey(new Date().getHours())];

  return (
    <div className="landing-home">
      <div>
        <h1 className="landing-home__title">{greeting}</h1>
        <p className="landing-home__subtitle">
          {t(
            "landing.home.subtitle",
            "Drop a PDF anywhere, or start from one of these.",
          )}
        </p>
      </div>

      <div className="landing-home__actions">
        {actions.map((action) => (
          <button
            key={action.id}
            type="button"
            className="landing-home__card"
            onClick={action.onClick}
          >
            <span className="landing-home__card-icon">
              <Icon name={action.icon} size="1.125rem" />
            </span>
            <span className="landing-home__card-title">{action.title}</span>
            <span className="landing-home__card-hint">{action.hint}</span>
          </button>
        ))}
      </div>

      <CreateProcessingFolderButton />

      {recent.length > 0 && (
        <div>
          <div className="landing-home__section-head">
            <h2 className="landing-home__section-title">
              {t("landing.home.recent", "Recent")}
            </h2>
            <button
              type="button"
              className="landing-home__link"
              onClick={() => openFilesModal()}
            >
              {t("landing.home.library", "Open file library")}
            </button>
          </div>
          <div className="landing-home__recent">
            {recent.map((file) => (
              <button
                key={file.id}
                type="button"
                className="landing-home__file"
                onClick={() => void fileActions.addScriptaFileStubs([file])}
              >
                <span className="landing-home__thumb">
                  {file.thumbnailUrl ? (
                    <img src={file.thumbnailUrl} alt="" />
                  ) : (
                    <Icon name="file-text" size="1.5rem" />
                  )}
                </span>
                <span className="landing-home__file-name">{file.name}</span>
                <span className="landing-home__file-when">
                  {formatWhen(file.lastModified, i18n.language)}
                </span>
              </button>
            ))}
          </div>
        </div>
      )}

      <ScanModal opened={scanOpen} onClose={() => setScanOpen(false)} />
    </div>
  );
}
