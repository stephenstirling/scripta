import { ColorInput, NumberInput, Select, Stack } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { Z_INDEX_AUTOMATE_DROPDOWN } from "@app/styles/zIndex";

export type StampFontType = "helvetica" | "times" | "courier";
export type StampMargin = "small" | "medium" | "large" | "x-large";

export interface StampAppearance {
  fontType: StampFontType;
  fontSize: number;
  customMargin: StampMargin;
  fontColor: string;
}

interface StampAppearanceFieldsProps {
  value: StampAppearance;
  onChange: (
    key: keyof StampAppearance,
    value: StampAppearance[keyof StampAppearance],
  ) => void;
  disabled?: boolean;
}

/** Font, size, colour and margin for text stamped onto pages (headers, Bates labels). */
const StampAppearanceFields = ({
  value,
  onChange,
  disabled = false,
}: StampAppearanceFieldsProps) => {
  const { t } = useTranslation();
  const comboboxProps = { withinPortal: true, zIndex: Z_INDEX_AUTOMATE_DROPDOWN };

  return (
    <Stack gap="md">
      <Select
        label={t("stampAppearance.font", "Font")}
        value={value.fontType}
        onChange={(v) =>
          onChange("fontType", (v as StampFontType | null) ?? "helvetica")
        }
        data={[
          { value: "helvetica", label: "Helvetica" },
          { value: "times", label: "Times Roman" },
          { value: "courier", label: "Courier" },
        ]}
        disabled={disabled}
        comboboxProps={comboboxProps}
      />
      <NumberInput
        label={t("stampAppearance.fontSize", "Font size")}
        value={value.fontSize}
        onChange={(v) => onChange("fontSize", typeof v === "number" ? v : 10)}
        min={1}
        max={72}
        disabled={disabled}
      />
      <ColorInput
        label={t("stampAppearance.color", "Colour")}
        value={value.fontColor}
        onChange={(v) => onChange("fontColor", v)}
        format="hex"
        disabled={disabled}
        popoverProps={{ withinPortal: true, zIndex: Z_INDEX_AUTOMATE_DROPDOWN }}
      />
      <Select
        label={t("stampAppearance.margin", "Distance from edge")}
        value={value.customMargin}
        onChange={(v) =>
          onChange("customMargin", (v as StampMargin | null) ?? "medium")
        }
        data={[
          { value: "small", label: t("sizes.small", "Small") },
          { value: "medium", label: t("sizes.medium", "Medium") },
          { value: "large", label: t("sizes.large", "Large") },
          { value: "x-large", label: t("sizes.x-large", "Extra Large") },
        ]}
        disabled={disabled}
        comboboxProps={comboboxProps}
      />
    </Stack>
  );
};

export default StampAppearanceFields;
