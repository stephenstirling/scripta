import { ReactNode } from "react";
import { useBanner } from "@app/contexts/BannerContext";
import NavigationWarningModal from "@app/components/shared/NavigationWarningModal";
import { TitleBarSearch } from "@app/components/layout/TitleBarSearch";
import { TitleBarDocumentTabs } from "@app/components/layout/TitleBarDocumentTabs";
import { TitleBarMenu } from "@app/components/layout/TitleBarMenu";

interface AppLayoutProps {
  children: ReactNode;
}

/**
 * App layout wrapper that handles banner rendering and viewport sizing
 * Automatically adjusts child components to fit remaining space after banner
 */
export function AppLayout({ children }: AppLayoutProps) {
  const { banner } = useBanner();

  return (
    <>
      <style>{`
        .h-screen {
          height: 100% !important;
        }
      `}</style>
      <div
        style={{
          height: "var(--titlebar-body-h, 100dvh)",
          display: "flex",
          flexDirection: "column",
        }}
      >
        {banner}
        <div style={{ flex: 1, minHeight: 0, height: 0 }}>{children}</div>
      </div>
      {/* Portal into the title-bar strip while it shows. Kept here so they stay
          mounted across views. */}
      <TitleBarMenu />
      <TitleBarSearch />
      <TitleBarDocumentTabs />
      <NavigationWarningModal />
    </>
  );
}
