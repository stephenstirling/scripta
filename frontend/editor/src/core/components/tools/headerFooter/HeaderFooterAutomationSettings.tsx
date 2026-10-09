import { Divider, Stack } from "@mantine/core";
import HeaderFooterSettings from "@app/components/tools/headerFooter/HeaderFooterSettings";
import StampAppearanceFields from "@app/components/tools/shared/StampAppearanceFields";
import type { HeaderFooterParameters } from "@app/components/tools/headerFooter/useHeaderFooterParameters";

interface HeaderFooterAutomationSettingsProps {
  parameters: HeaderFooterParameters;
  onParameterChange: <K extends keyof HeaderFooterParameters>(
    key: K,
    value: HeaderFooterParameters[K],
  ) => void;
  disabled?: boolean;
}

const HeaderFooterAutomationSettings = ({
  parameters,
  onParameterChange,
  disabled = false,
}: HeaderFooterAutomationSettingsProps) => (
  <Stack gap="lg">
    <HeaderFooterSettings
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

export default HeaderFooterAutomationSettings;
