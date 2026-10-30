import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useViewScopedFiles } from "@app/hooks/tools/shared/useViewScopedFiles";
import {
  createToolFlow,
  type MiddleStepConfig,
} from "@app/components/tools/shared/createToolFlow";
import { BaseToolProps, ToolComponent } from "@app/types/tool";
import { useEndpointEnabled } from "@app/hooks/useEndpointConfig";
import { useAccordionSteps } from "@app/hooks/tools/shared/useAccordionSteps";
import { useHeaderFooterParameters } from "@app/components/tools/headerFooter/useHeaderFooterParameters";
import { useHeaderFooterOperation } from "@app/components/tools/headerFooter/useHeaderFooterOperation";
import HeaderFooterSettings from "@app/components/tools/headerFooter/HeaderFooterSettings";
import StampAppearanceFields from "@app/components/tools/shared/StampAppearanceFields";

enum HeaderFooterStep {
  NONE = "none",
  TEXT = "text",
  APPEARANCE = "appearance",
}

const HeaderFooter = ({
  onPreviewFile,
  onComplete,
  onError,
}: BaseToolProps) => {
  const { t } = useTranslation();
  const selectedFiles = useViewScopedFiles();
  const params = useHeaderFooterParameters();
  const operation = useHeaderFooterOperation();
  const { enabled: endpointEnabled, loading: endpointLoading } =
    useEndpointEnabled("add-header-footer");

  useEffect(() => {
    operation.resetResults();
    onPreviewFile?.(null);
  }, [params.parameters]);

  const handleExecute = async () => {
    try {
      await operation.executeOperation(params.parameters, selectedFiles);
      if (operation.files && onComplete) {
        onComplete(operation.files);
      }
    } catch (error) {
      onError?.(
        (error instanceof Error ? error.message : undefined) ||
          t(
            "headerFooter.error.failed",
            "An error occurred while adding headers and footers.",
          ),
      );
    }
  };

  const hasFiles = selectedFiles.length > 0;
  const hasResults =
    operation.files.length > 0 || operation.downloadUrl !== null;

  const accordion = useAccordionSteps<HeaderFooterStep>({
    noneValue: HeaderFooterStep.NONE,
    initialStep: HeaderFooterStep.TEXT,
    stateConditions: { hasFiles, hasResults },
    afterResults: () => {
      operation.resetResults();
      onPreviewFile?.(null);
    },
  });

  const steps: MiddleStepConfig[] = [
    {
      title: t("headerFooter.steps.text", "Header & footer text"),
      isCollapsed: accordion.getCollapsedState(HeaderFooterStep.TEXT),
      onCollapsedClick: () => accordion.handleStepToggle(HeaderFooterStep.TEXT),
      isVisible: hasFiles || hasResults,
      content: (
        <HeaderFooterSettings
          parameters={params.parameters}
          onParameterChange={params.updateParameter}
          disabled={endpointLoading}
        />
      ),
    },
    {
      title: t("headerFooter.steps.appearance", "Appearance"),
      isCollapsed: accordion.getCollapsedState(HeaderFooterStep.APPEARANCE),
      onCollapsedClick: () =>
        accordion.handleStepToggle(HeaderFooterStep.APPEARANCE),
      isVisible: hasFiles || hasResults,
      content: (
        <StampAppearanceFields
          value={params.parameters}
          onChange={params.updateParameter}
          disabled={endpointLoading}
        />
      ),
    },
  ];

  return createToolFlow({
    files: {
      selectedFiles:
        operation.getEligibleFiles?.(params.parameters, selectedFiles) ??
        selectedFiles,
      isCollapsed: hasResults,
    },
    steps,
    executeButton: {
      text: t("headerFooter.submit", "Add Header & Footer"),
      isVisible: !hasResults,
      loadingText: t("loading"),
      onClick: handleExecute,
      endpointEnabled,
      paramsValid: params.validateParameters(),
    },
    review: {
      isVisible: hasResults,
      operation,
      title: t("headerFooter.results.title", "Header & footer results"),
      onFileClick: (file) => onPreviewFile?.(file),
      onUndo: async () => {
        await operation.undoOperation();
        onPreviewFile?.(null);
      },
    },
  });
};

HeaderFooter.tool = () => useHeaderFooterOperation;

export default HeaderFooter as ToolComponent;
