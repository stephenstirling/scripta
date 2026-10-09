import { useTranslation } from "react-i18next";
import {
  useToolOperation,
  defineSingleFileTool,
} from "@app/hooks/tools/shared/useToolOperation";
import {
  objectToFormData,
  type ToolApiParams,
  type ToolEndpoint,
} from "@app/hooks/tools/shared/toolApiMapping";
import { createStandardErrorHandler } from "@app/utils/toolErrorHandler";
import {
  HeaderFooterParameters,
  defaultParameters,
  validateHeaderFooterParameters,
} from "@app/components/tools/headerFooter/useHeaderFooterParameters";

const ENDPOINT = "/api/v1/misc/add-header-footer" satisfies ToolEndpoint;
type HeaderFooterApiParams = ToolApiParams[typeof ENDPOINT];

export const headerFooterToApiParams = (
  p: HeaderFooterParameters,
): HeaderFooterApiParams => ({
  headerLeft: p.headerLeft,
  headerCenter: p.headerCenter,
  headerRight: p.headerRight,
  footerLeft: p.footerLeft,
  footerCenter: p.footerCenter,
  footerRight: p.footerRight,
  pagesToApply: p.pagesToApply,
  startingNumber: p.startingNumber,
  dateFormat: p.dateFormat,
  fontType: p.fontType,
  fontSize: p.fontSize,
  customMargin: p.customMargin,
  fontColor: p.fontColor,
});

export const headerFooterFromApiParams = (
  a: HeaderFooterApiParams,
): Partial<HeaderFooterParameters> => ({
  headerLeft: a.headerLeft ?? "",
  headerCenter: a.headerCenter ?? "",
  headerRight: a.headerRight ?? "",
  footerLeft: a.footerLeft ?? "",
  footerCenter: a.footerCenter ?? "",
  footerRight: a.footerRight ?? "",
  pagesToApply: a.pagesToApply ?? defaultParameters.pagesToApply,
  startingNumber: a.startingNumber ?? defaultParameters.startingNumber,
  dateFormat: a.dateFormat ?? defaultParameters.dateFormat,
  fontType: a.fontType ?? defaultParameters.fontType,
  fontSize: a.fontSize ?? defaultParameters.fontSize,
  customMargin: a.customMargin ?? defaultParameters.customMargin,
  fontColor: a.fontColor ?? defaultParameters.fontColor,
});

export const buildHeaderFooterFormData = (
  parameters: HeaderFooterParameters,
  file: File,
): FormData =>
  objectToFormData(headerFooterToApiParams(parameters), { fileInput: file });

export const headerFooterOperationConfig = defineSingleFileTool({
  validateParams: validateHeaderFooterParameters,
  buildFormData: buildHeaderFooterFormData,
  toApiParams: headerFooterToApiParams,
  fromApiParams: headerFooterFromApiParams,
  operationType: "headerFooter",
  endpoint: ENDPOINT,
  defaultParameters,
});

export const useHeaderFooterOperation = () => {
  const { t } = useTranslation();
  return useToolOperation<HeaderFooterParameters>({
    ...headerFooterOperationConfig,
    getErrorMessage: createStandardErrorHandler(
      t(
        "headerFooter.error.failed",
        "An error occurred while adding headers and footers.",
      ),
    ),
  });
};
