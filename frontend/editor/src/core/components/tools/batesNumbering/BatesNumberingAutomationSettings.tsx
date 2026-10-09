import { Divider, Stack } from "@mantine/core";
import BatesNumberingSettings from "@app/components/tools/batesNumbering/BatesNumberingSettings";
import StampAppearanceFields from "@app/components/tools/shared/StampAppearanceFields";
import type { BatesNumberingParameters } from "@app/components/tools/batesNumbering/useBatesNumberingParameters";

interface BatesNumberingAutomationSettingsProps {
  parameters: BatesNumberingParameters;
  onParameterChange: <K extends keyof BatesNumberingParameters>(
    key: K,
    value: BatesNumberingParameters[K],
  ) => void;
  disabled?: boolean;
}

const BatesNumberingAutomationSettings = ({
  parameters,
  onParameterChange,
  disabled = false,
}: BatesNumberingAutomationSettingsProps) => (
  <Stack gap="lg">
    <BatesNumberingSettings
      parameters={parameters}
      onParameterChange={onParameterChange}
      disabled={disabled}
    />
    <Divider />
    <StampAppearanceFields
      value={parameters}
      onChange={onParameterChange}
      disabled={disabled}
    />
  </Stack>
);

export default BatesNumberingAutomationSettings;
