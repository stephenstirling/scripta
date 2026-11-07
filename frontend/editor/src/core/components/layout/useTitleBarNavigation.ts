import { useNavigate } from "react-router-dom";
import { useQuickNavHost } from "@app/contexts/QuickNavHostContext";
import { EDITOR_BASENAME } from "@app/routes/editorBasename";
import { rememberSettingsOrigin } from "@app/utils/settingsNavigation";

/**
 * Where the title-bar strip sends you. Every move goes through the app's own
 * navigation guard when it supplies one, so leaving mid-edit still prompts.
 */
export function useTitleBarNavigation() {
  const navigate = useNavigate();
  const host = useQuickNavHost();

  const guarded = (leave: () => void) => {
    const guard = host?.actions.current?.requestNavigation;
    if (guard) guard(leave);
    else leave();
  };

  return {
    guarded,
    navigate,
    host,
    goHome: () =>
      guarded(() => {
        const actions = host?.actions.current;
        const start = actions?.goToStartupView ?? actions?.goToDefaultState;
        if (start) start();
        else navigate(EDITOR_BASENAME);
      }),
    showLibrary: () =>
      guarded(() => {
        const show = host?.actions.current?.showFileLibrary;
        if (show) show();
        else navigate("/files");
      }),
    openSettings: () => {
      rememberSettingsOrigin();
      guarded(() => navigate("/settings/general"));
    },
  };
}
