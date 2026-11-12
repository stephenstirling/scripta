import { useId } from "react";
import { Select, Switch } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { SegmentedControl } from "@app/ui/SegmentedControl";
import { usePreferences } from "@app/contexts/PreferencesContext";
import type { ToolPanelMode } from "@app/constants/toolPanel";
import {
  type StartupView,
  type ViewerZoomSetting,
} from "@app/services/preferencesService";
import { Z_INDEX_OVER_CONFIG_MODAL } from "@app/styles/zIndex";
import { SettingsRow } from "@app/components/shared/config/SettingsRow";

/** What the editor opens with, and which tools it bothers to list. */
export function EditorDefaultsCard() {
  const { t } = useTranslation();
  const labelIds = useId();
  const viewerZoomLabelId = `${labelIds}-viewer-zoom`;
  const hideToolsLabelId = `${labelIds}-hide-tools`;
  const hideConversionsLabelId = `${labelIds}-hide-conversions`;
  const { preferences, updatePreference } = usePreferences();

  return (
    <div className="settings-group">
      <SettingsRow
        id="setting-tool-picker-mode"
        label={t(
          "settings.general.defaultToolPickerMode",
          "Default tool picker mode",
        )}
        description={t(
          "settings.general.defaultToolPickerModeDescription",
          "Choose whether the tool picker opens in fullscreen or sidebar by default",
        )}
      >
        <SegmentedControl
          value={preferences.defaultToolPanelMode}
          onChange={(val: string) =>
            updatePreference("defaultToolPanelMode", val as ToolPanelMode)
          }
          options={[
            {
              label: t("settings.general.mode.sidebar", "Sidebar"),
              value: "sidebar",
            },
            {
              label: t("settings.general.mode.fullscreen", "Fullscreen"),
              value: "fullscreen",
            },
          ]}
        />
      </SettingsRow>
      <SettingsRow
        id="setting-startup-view"
        label={t(
          "settings.general.defaultStartupView",
          "Default view on launch",
        )}
        description={t(
          "settings.general.defaultStartupViewDescription",
          "Choose which view is active when the app starts",
        )}
      >
        <SegmentedControl
          value={preferences.defaultStartupView}
          onChange={(val: string) =>
            updatePreference("defaultStartupView", val as StartupView)
          }
          options={[
            {
              label: t("settings.general.startupView.tools", "Tools"),
              value: "tools",
            },
            {
              label: t("settings.general.startupView.read", "Reader"),
              value: "read",
            },
            {
              label: t("settings.general.startupView.automate", "Automate"),
              value: "automate",
            },
          ]}
        />
      </SettingsRow>
      <SettingsRow
        id="setting-reader-zoom"
        labelId={viewerZoomLabelId}
        label={t("settings.general.defaultViewerZoom", "Default reader zoom")}
        description={t(
          "settings.general.defaultViewerZoomDescription",
          "Set the default zoom level when opening PDFs in the reader",
        )}
      >
        <Select
          aria-labelledby={viewerZoomLabelId}
          value={preferences.defaultViewerZoom}
          onChange={(val: string | null) => {
            if (val)
              updatePreference("defaultViewerZoom", val as ViewerZoomSetting);
          }}
          data={[
            {
              label: t("settings.general.zoomLevel.auto", "Auto"),
              value: "auto",
            },
            {
              label: t("settings.general.zoomLevel.fitWidth", "Fit width"),
              value: "fitWidth",
            },
            {
              label: t("settings.general.zoomLevel.fitPage", "Fit page"),
              value: "fitPage",
            },
            { label: "50%", value: "50" },
            { label: "75%", value: "75" },
            { label: "100%", value: "100" },
            { label: "125%", value: "125" },
            { label: "150%", value: "150" },
            { label: "200%", value: "200" },
          ]}
          style={{ width: 140 }}
          allowDeselect={false}
          comboboxProps={{
            withinPortal: true,
            zIndex: Z_INDEX_OVER_CONFIG_MODAL,
          }}
        />
      </SettingsRow>
      <SettingsRow
        id="setting-hide-unavailable-tools"
        labelId={hideToolsLabelId}
        label={t(
          "settings.general.hideUnavailableTools",
          "Hide unavailable tools",
        )}
        description={t(
          "settings.general.hideUnavailableToolsDescription",
          "Remove tools that have been disabled by your server instead of showing them greyed out.",
        )}
      >
        <Switch
          aria-labelledby={hideToolsLabelId}
          checked={preferences.hideUnavailableTools}
          onChange={(event) =>
            updatePreference(
              "hideUnavailableTools",
              event.currentTarget.checked,
            )
          }
        />
      </SettingsRow>
      <SettingsRow
        id="setting-hide-unavailable-conversions"
        labelId={hideConversionsLabelId}
        label={t(
          "settings.general.hideUnavailableConversions",
          "Hide unavailable conversions",
        )}
        description={t(
          "settings.general.hideUnavailableConversionsDescription",
          "Remove disabled conversion options in the Convert tool instead of showing them greyed out.",
        )}
      >
        <Switch
          aria-labelledby={hideConversionsLabelId}
          checked={preferences.hideUnavailableConversions}
          onChange={(event) =>
            updatePreference(
              "hideUnavailableConversions",
              event.currentTarget.checked,
            )
          }
        />
      </SettingsRow>
    </div>
  );
}
