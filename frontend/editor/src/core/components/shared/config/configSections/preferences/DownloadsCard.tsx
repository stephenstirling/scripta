import { useEffect, useId, useState } from "react";
import { NumberInput, Switch } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { usePreferences } from "@app/contexts/PreferencesContext";
import { SettingsRow } from "@app/components/shared/config/SettingsRow";

const DEFAULT_AUTO_UNZIP_FILE_LIMIT = 4;

/** What happens to ZIPs the API hands back. Automations are unaffected. */
export function DownloadsCard() {
  const { t } = useTranslation();
  const labelIds = useId();
  const autoUnzipLabelId = `${labelIds}-auto-unzip`;
  const autoUnzipLimitLabelId = `${labelIds}-auto-unzip-limit`;
  const { preferences, updatePreference } = usePreferences();
  const [fileLimitInput, setFileLimitInput] = useState<number | string>(
    preferences.autoUnzipFileLimit,
  );

  useEffect(() => {
    setFileLimitInput(preferences.autoUnzipFileLimit);
  }, [preferences.autoUnzipFileLimit]);

  return (
    <div className="settings-group">
      <SettingsRow
        id="setting-auto-unzip"
        labelId={autoUnzipLabelId}
        label={t("settings.general.autoUnzip", "Auto-unzip API responses")}
        description={t(
          "settings.general.autoUnzipTooltip",
          "Automatically extract ZIP files returned from API operations. Disable to keep ZIP files intact. This does not affect automation workflows.",
        )}
      >
        <Switch
          aria-labelledby={autoUnzipLabelId}
          checked={preferences.autoUnzip}
          onChange={(event) =>
            updatePreference("autoUnzip", event.currentTarget.checked)
          }
        />
      </SettingsRow>
      <SettingsRow
        id="setting-auto-unzip-file-limit"
        labelId={autoUnzipLimitLabelId}
        label={t(
          "settings.general.autoUnzipFileLimit",
          "Auto-unzip file limit",
        )}
        description={t(
          "settings.general.autoUnzipFileLimitTooltip",
          "Only unzip if the ZIP contains this many files or fewer. Set higher to extract larger ZIPs.",
        )}
      >
        <NumberInput
          aria-labelledby={autoUnzipLimitLabelId}
          value={fileLimitInput}
          onChange={setFileLimitInput}
          onBlur={() => {
            const numValue = Number(fileLimitInput);
            const finalValue =
              !fileLimitInput ||
              isNaN(numValue) ||
              numValue < 1 ||
              numValue > 100
                ? DEFAULT_AUTO_UNZIP_FILE_LIMIT
                : numValue;
            setFileLimitInput(finalValue);
            updatePreference("autoUnzipFileLimit", finalValue);
          }}
          min={1}
          max={100}
          step={1}
          disabled={!preferences.autoUnzip}
          style={{ width: 90 }}
        />
      </SettingsRow>
    </div>
  );
}
