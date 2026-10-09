export type OnboardingStepId =
  | "first-login"
  | "welcome"
  | "processor-intro"
  | "desktop-install"
  | "security-check"
  | "admin-overview"
  | "tool-layout"
  | "tour-overview"
  | "server-license"
  | "analytics-choice"
  | "mfa-setup";

export type OnboardingStepType = "modal-slide" | "tool-prompt";

export interface OnboardingRuntimeState {
  selectedRole: "admin" | "user" | null;
  tourRequested: boolean;
  // Open key into the tour registry (see tourRegistry.ts).
  tourType: string;
  isDesktopApp: boolean;
  desktopSlideEnabled: boolean;
  analyticsNotConfigured: boolean;
  analyticsEnabled: boolean;
  licenseNotice: {
    totalUsers: number | null;
    freeTierLimit: number;
    isOverLimit: boolean;
    requiresLicense: boolean;
  };
  requiresPasswordChange: boolean;
  firstLoginUsername: string;
  usingDefaultCredentials: boolean;
  requiresMfaSetup: boolean;
}

export interface OnboardingConditionContext extends OnboardingRuntimeState {
  loginEnabled: boolean;
  effectiveIsAdmin: boolean;
}

export interface OnboardingStep {
  id: OnboardingStepId;
  type: OnboardingStepType;
  condition: (ctx: OnboardingConditionContext) => boolean;
  slideId?:
    | "first-login"
    | "welcome"
    | "processor-intro"
    | "desktop-install"
    | "security-check"
    | "admin-overview"
    | "server-license"
    | "tour-overview"
    | "analytics-choice"
    | "mfa-setup";
  allowDismiss?: boolean;
}

export const DEFAULT_RUNTIME_STATE: OnboardingRuntimeState = {
  selectedRole: null,
  tourRequested: false,
  tourType: "whatsnew",
  isDesktopApp: false,
  analyticsNotConfigured: false,
  analyticsEnabled: false,
  licenseNotice: {
    totalUsers: null,
    freeTierLimit: 5,
    isOverLimit: false,
    requiresLicense: false,
  },
  requiresPasswordChange: false,
  firstLoginUsername: "",
  usingDefaultCredentials: false,
  desktopSlideEnabled: true,
  requiresMfaSetup: false,
};

// Scripta omits the upstream slides that promote paid plans, the hosted
// Processor and the separate desktop installer.
export const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    id: "welcome",
    type: "modal-slide",
    slideId: "welcome",
    // Desktop has its own onboarding modal (DesktopOnboardingModal)
    condition: (ctx) => !ctx.isDesktopApp,
  },
  {
    id: "tour-overview",
    type: "modal-slide",
    slideId: "tour-overview",
    condition: (ctx) =>
      !ctx.effectiveIsAdmin && ctx.tourType !== "admin" && !ctx.isDesktopApp,
  },
];

export function getStepById(id: OnboardingStepId): OnboardingStep | undefined {
  return ONBOARDING_STEPS.find((step) => step.id === id);
}

export function getStepIndex(id: OnboardingStepId): number {
  return ONBOARDING_STEPS.findIndex((step) => step.id === id);
}
