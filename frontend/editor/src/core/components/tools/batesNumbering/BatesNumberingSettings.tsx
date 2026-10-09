import {
  Checkbox,
  Group,
  NumberInput,
  Select,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { useTranslation } from "react-i18next";
import { Z_INDEX_AUTOMATE_DROPDOWN } from "@app/styles/zIndex";
import {
  BATES_MAX_DIGITS,
  formatBatesLabel,
  type BatesNumberingParameters,
  type BatesPosition,
} from "@app/components/tools/batesNumbering/useBatesNumberingParameters";

interface BatesNumberingSettingsProps {
  parameters: BatesNumberingParameters;
  onParameterChange: <K extends keyof BatesNumberingParameters>(
    key: K,
    value: BatesNumberingParameters[K],
  ) => void;
  disabled?: boolean;
}

/** Prefix, number format, position and output naming for Bates numbering. */
const BatesNumberingSettings = ({
  parameters,
  onParameterChange,
  disabled = false,
}: BatesNumberingSettingsProps) => {
  const { t } = useTranslation();

  const positions: { value: string; label: string }[] = [
    { value: "1", label: t("batesNumbering.position.1", "Top left") },
    { value: "2", label: t("batesNumbering.position.2", "Top centre") },
    { value: "3", label: t("batesNumbering.position.3", "Top right") },
    { value: "7", label: t("batesNumbering.position.7", "Bottom left") },
    { value: "8", label: t("batesNumbering.position.8", "Bottom centre") },
    { value: "9", label: t("batesNumbering.position.9", "Bottom right") },
  ];

  return (
    <Stack gap="md">
      <Group grow>
        <TextInput
          label={t("batesNumbering.prefix", "Prefix")}
          placeholder="ACME"
          value={parameters.prefix}
          onChange={(e) => onParameterChange("prefix", e.currentTarget.value)}
          disabled={disabled}
        />
        <TextInput
          label={t("batesNumbering.suffix", "Suffix")}
          value={parameters.suffix}
          onChange={(e) => onParameterChange("suffix", e.currentTarget.value)}
          disabled={disabled}
        />
      </Group>
      <Group grow>
        <NumberInput
          label={t("batesNumbering.startNumber", "Start at")}
          value={parameters.startNumber}
          onChange={(v) =>
            onParameterChange("startNumber", typeof v === "number" ? v : 1)
          }
          min={0}
          allowDecimal={false}
          disabled={disabled}
        />
        <NumberInput
          label={t("batesNumbering.digits", "Digits")}
          value={parameters.digits}
          onChange={(v) =>
            onParameterChange("digits", typeof v === "number" ? v : 6)
          }
          min={1}
          max={BATES_MAX_DIGITS}
          allowDecimal={false}
          disabled={disabled}
        />
      </Group>
      <Text size="sm">
        {t("batesNumbering.preview", "First page: {{label}}", {
          label: formatBatesLabel(parameters, parameters.startNumber),
        })}
      </Text>
      <Select
        label={t("batesNumbering.positionLabel", "Position")}
        value={String(parameters.position)}
        onChange={(v) =>
          onParameterChange("position", Number(v ?? 9) as BatesPosition)
        }
        data={positions}
        disabled={disabled}
        comboboxProps={{
          withinPortal: true,
          zIndex: Z_INDEX_AUTOMATE_DROPDOWN,
        }}
      />
      <Checkbox
        label={t(
          "batesNumbering.nameByRange",
          "Name each file after its Bates range",
        )}
        checked={parameters.nameByRange}
        onChange={(e) =>
          onParameterChange("nameByRange", e.currentTarget.checked)
        }
        disabled={disabled}
      />
      <Text size="xs" c="dimmed">
        {t(
          "batesNumbering.orderHint",
          "Numbering continues across files in the order they are listed.",
        )}
      </Text>
    </Stack>
  );
};

export default BatesNumberingSettings;
