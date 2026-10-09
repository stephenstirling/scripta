import { useTranslation } from "react-i18next";
import {
  useToolOperation,
  defineMultiFileTool,
} from "@app/hooks/tools/shared/useToolOperation";
import {
  objectToFormData,
  type ToolApiParams,
  type ToolEndpoint,
} from "@app/hooks/tools/shared/toolApiMapping";
import { createStandardErrorHandler } from "@app/utils/toolErrorHandler";
import {
  BatesNumberingParameters,
  defaultParameters,
  validateBatesNumberingParameters,
} from "@app/components/tools/batesNumbering/useBatesNumberingParameters";

const ENDPOINT = "/api/v1/misc/add-bates-numbers" satisfies ToolEndpoint;
type BatesApiParams = ToolApiParams[typeof ENDPOINT];

export const batesToApiParams = (
  p: BatesNumberingParameters,
): BatesApiParams => ({
  prefix: p.prefix,
  suffix: p.suffix,
  startNumber: p.startNumber,
  digits: p.digits,
  position: p.position,
  nameByRange: p.nameByRange,
  fontType: p.fontType,
  fontSize: p.fontSize,
  customMargin: p.customMargin,
  fontColor: p.fontColor,
});

export const batesFromApiParams = (
  a: BatesApiParams,
): Partial<BatesNumberingParameters> => ({
  prefix: a.prefix ?? defaultParameters.prefix,
  suffix: a.suffix ?? defaultParameters.suffix,
  startNumber: a.startNumber ?? defaultParameters.startNumber,
  digits: a.digits ?? defaultParameters.digits,
  position: a.position ?? defaultParameters.position,
  nameByRange: a.nameByRange ?? defaultParameters.nameByRange,
  fontType: a.fontType ?? defaultParameters.fontType,
  fontSize: a.fontSize ?? defaultParameters.fontSize,
  customMargin: a.customMargin ?? defaultParameters.customMargin,
  fontColor: a.fontColor ?? defaultParameters.fontColor,
});

// Numbering runs across the files in the order they are sent, so all files go
// in one request, in the user's order.
const buildFormData = (
  parameters: BatesNumberingParameters,
  files: File[],
): FormData =>
  objectToFormData(batesToApiParams(parameters), { fileInput: files });

export const batesNumberingOperationConfig = defineMultiFileTool({
  validateParams: validateBatesNumberingParameters,
  buildFormData,
  toApiParams: batesToApiParams,
  fromApiParams: batesFromApiParams,
  operationType: "batesNumbering",
  endpoint: ENDPOINT,
  filePrefix: "bates_",
  defaultParameters,
});

export const useBatesNumberingOperation = () => {
  const { t } = useTranslation();
  return useToolOperation<BatesNumberingParameters>({
    ...batesNumberingOperationConfig,
    getErrorMessage: createStandardErrorHandler(
      t(
        "batesNumbering.error.failed",
        "An error occurred while adding Bates numbers.",
      ),
    ),
  });
};
