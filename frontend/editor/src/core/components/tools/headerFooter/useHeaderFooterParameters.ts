import { BaseParameters } from "@app/types/parameters";
import {
  useBaseParameters,
  type BaseParametersHook,
} from "@app/hooks/tools/shared/useBaseParameters";
import type { StampAppearance } from "@app/components/tools/shared/StampAppearanceFields";

export const HEADER_FOOTER_SLOTS = [
  "headerLeft",
  "headerCenter",
  "headerRight",
  "footerLeft",
  "footerCenter",
  "footerRight",
] as const;
export type HeaderFooterSlot = (typeof HEADER_FOOTER_SLOTS)[number];

export interface HeaderFooterParameters
  extends BaseParameters, StampAppearance, Record<HeaderFooterSlot, string> {
  pagesToApply: string;
  startingNumber: number;
  dateFormat: string;
}

export const defaultParameters: HeaderFooterParameters = {
  headerLeft: "",
  headerCenter: "",
  headerRight: "",
  footerLeft: "",
  footerCenter: "Page {n} of {total}",
  footerRight: "",
  pagesToApply: "",
  startingNumber: 1,
  dateFormat: "yyyy-MM-dd",
  fontType: "helvetica",
  fontSize: 10,
  customMargin: "medium",
  fontColor: "#000000",
};

export type HeaderFooterParametersHook =
  BaseParametersHook<HeaderFooterParameters>;

export function validateHeaderFooterParameters(
  params: HeaderFooterParameters,
): boolean {
  const hasText = HEADER_FOOTER_SLOTS.some(
    (slot) => params[slot].trim().length > 0,
  );
  return hasText && params.fontSize > 0 && params.startingNumber >= 0;
}

export const useHeaderFooterParameters = (): HeaderFooterParametersHook =>
  useBaseParameters<HeaderFooterParameters>({
    defaultParameters,
    endpointName: "add-header-footer",
    validateFn: validateHeaderFooterParameters,
  });
