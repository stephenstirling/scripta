import {
  NumberInput,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { useTranslation } from "react-i18next";
import type {
  HeaderFooterParameters,
  HeaderFooterSlot,
} from "@app/components/tools/headerFooter/useHeaderFooterParameters";

interface HeaderFooterSettingsProps {
  parameters: HeaderFooterParameters;
  onParameterChange: <K extends keyof HeaderFooterParameters>(
    key: K,
    value: HeaderFooterParameters[K],
  ) => void;
  disabled?: boolean;
}

/** The six header/footer text slots plus page range, numbering and date options. */
const HeaderFooterSettings = ({
  parameters,
  onParameterChange,
  disabled = false,
}: HeaderFooterSettingsProps) => {
  const { t } = useTranslation();

  const slotInput = (slot: HeaderFooterSlot, label: string) => (
    <TextInput
      key={slot}
      label={label}
      value={parameters[slot]}
      onChange={(e) => onParameterChange(slot, e.currentTarget.value)}
      disabled={disabled}
    />
  );

  return (
    <Stack gap="md">
      <Text size="sm" fw={600}>
        {t("headerFooter.header", "Header")}
      </Text>
      <SimpleGrid cols={3} spacing="xs">
        {slotInput("headerLeft", t("headerFooter.left", "Left"))}
        {slotInput("headerCenter", t("headerFooter.center", "Centre"))}
        {slotInput("headerRight", t("headerFooter.right", "Right"))}
      </SimpleGrid>
      <Text size="sm" fw={600}>
        {t("headerFooter.footer", "Footer")}
      </Text>
      <SimpleGrid cols={3} spacing="xs">
        {slotInput("footerLeft", t("headerFooter.left", "Left"))}
        {slotInput("footerCenter", t("headerFooter.center", "Centre"))}
        {slotInput("footerRight", t("headerFooter.right", "Right"))}
      </SimpleGrid>
      <Text size="xs" c="dimmed">
        {t(
          "headerFooter.tokens",
          "Use {n} for the page number, {total} for the page count, {filename} for the file name and {date} for today's date.",
        )}
      </Text>
      <TextInput
        label={t("headerFooter.pages", "Pages")}
        placeholder={t("headerFooter.pagesPlaceholder", "All pages, or e.g. 1,3-5")}
        value={parameters.pagesToApply}
        onChange={(e) =>
          onParameterChange("pagesToApply", e.currentTarget.value)
        }
        disabled={disabled}
      />
      <NumberInput
        label={t("headerFooter.startingNumber", "First page number")}
        value={parameters.startingNumber}
        onChange={(v) =>
          onParameterChange("startingNumber", typeof v === "number" ? v : 1)
        }
        min={0}
        disabled={disabled}
      />
      <TextInput
        label={t("headerFooter.dateFormat", "Date format")}
        description={t(
          "headerFooter.dateFormatDesc",
          "For {date}, e.g. yyyy-MM-dd or dd MMM yyyy",
        )}
        value={parameters.dateFormat}
        onChange={(e) => onParameterChange("dateFormat", e.currentTarget.value)}
        disabled={disabled}
      />
    </Stack>
  );
};

export default HeaderFooterSettings;
