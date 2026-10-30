import { BaseParameters } from "@app/types/parameters";
import {
  useBaseParameters,
  type BaseParametersHook,
} from "@app/hooks/tools/shared/useBaseParameters";
import type { StampAppearance } from "@app/components/tools/shared/StampAppearanceFields";

export type BatesPosition = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;

export interface BatesNumberingParameters
  extends BaseParameters, StampAppearance {
  prefix: string;
  suffix: string;
  startNumber: number;
  digits: number;
  position: BatesPosition;
  nameByRange: boolean;
}

export const BATES_MAX_DIGITS = 15;

export const defaultParameters: BatesNumberingParameters = {
  prefix: "",
  suffix: "",
  startNumber: 1,
  digits: 6,
  position: 9,
  nameByRange: false,
  fontType: "helvetica",
  fontSize: 10,
  customMargin: "medium",
  fontColor: "#000000",
};

export type BatesNumberingParametersHook =
  BaseParametersHook<BatesNumberingParameters>;

export function validateBatesNumberingParameters(
  params: BatesNumberingParameters,
): boolean {
  return (
    params.startNumber >= 0 &&
    params.digits >= 1 &&
    params.digits <= BATES_MAX_DIGITS &&
    params.fontSize > 0
  );
}

/** The label the first page will carry, for previewing the settings. */
export function formatBatesLabel(
  params: Pick<BatesNumberingParameters, "prefix" | "suffix" | "digits">,
  value: number,
): string {
  const digits = Math.min(BATES_MAX_DIGITS, Math.max(1, params.digits));
  return `${params.prefix}${String(value).padStart(digits, "0")}${params.suffix}`;
}

export const useBatesNumberingParameters = (): BatesNumberingParametersHook =>
  useBaseParameters<BatesNumberingParameters>({
    defaultParameters,
    endpointName: "add-bates-numbers",
    validateFn: validateBatesNumberingParameters,
  });
