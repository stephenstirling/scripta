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
import { useBatesNumberingParameters } from "@app/components/tools/batesNumbering/useBatesNumberingParameters";
import { useBatesNumberingOperation } from "@app/components/tools/batesNumbering/useBatesNumberingOperation";
import BatesNumberingSettings from "@app/components/tools/batesNumbering/BatesNumberingSettings";
import StampAppearanceFields from "@app/components/tools/shared/StampAppearanceFields";

enum BatesStep {
  NONE = "none",
  TEXT = "text",
  APPEARANCE = "appearance",
}

const BatesNumbering = ({
  onPreviewFile,
  onComplete,
  onError,
}: BaseToolProps) => {
  const { t } = useTranslation();
  const selectedFiles = useViewScopedFiles();
  const params = useBatesNumberingParameters();
  const operation = useBatesNumberingOperation();
  const { enabled: endpointEnabled, loading: endpointLoading } =
    useEndpointEnabled("add-bates-numbers");

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
            "batesNumbering.error.failed",
            "An error occurred while adding Bates numbers.",
          ),
      );
    }
  };

  const hasFiles = selectedFiles.length > 0;
  const hasResults =
    operation.files.length > 0 || operation.downloadUrl !== null;

  const accordion = useAccordionSteps<BatesStep>({
    noneValue: BatesStep.NONE,
    initialStep: BatesStep.TEXT,
    stateConditions: { hasFiles, hasResults },
    afterResults: () => {
      operation.resetResults();
      onPreviewFile?.(null);
    },
  });

  const steps: MiddleStepConfig[] = [
    {
      title: t("batesNumbering.steps.numbering", "Numbering"),
      isCollapsed: accordion.getCollapsedState(BatesStep.TEXT),
      onCollapsedClick: () => accordion.handleStepToggle(BatesStep.TEXT),
      isVisible: hasFiles || hasResults,
      content: (
        <BatesNumberingSettings
          parameters={params.parameters}
          onParameterChange={params.updateParameter}
          disabled={endpointLoading}
        />
      ),
    },
    {
      title: t("batesNumbering.steps.appearance", "Appearance"),
      isCollapsed: accordion.getCollapsedState(BatesStep.APPEARANCE),
      onCollapsedClick: () => accordion.handleStepToggle(BatesStep.APPEARANCE),
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
      text: t("batesNumbering.submit", "Add Bates Numbers"),
      isVisible: !hasResults,
      loadingText: t("loading"),
      onClick: handleExecute,
      endpointEnabled,
      paramsValid: params.validateParameters(),
    },
    review: {
      isVisible: hasResults,
      operation,
      title: t("batesNumbering.results.title", "Bates numbering results"),
      onFileClick: (file) => onPreviewFile?.(file),
      onUndo: async () => {
        await operation.undoOperation();
        onPreviewFile?.(null);
      },
    },
  });
};

BatesNumbering.tool = () => useBatesNumberingOperation;

export default BatesNumbering as ToolComponent;
