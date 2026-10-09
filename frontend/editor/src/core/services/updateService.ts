import { APP_NAME, LATEST_RELEASE_API } from "@app/constants/branding";
import { DOWNLOAD_BASE_URL } from "@app/constants/downloads";

export interface UpdateSummary {
  latest_version: string | null;
  latest_stable_version?: string | null;
  max_priority: "urgent" | "normal" | "minor" | "low";
  recommended_action?: string;
  any_breaking: boolean;
  migration_guides?: Array<{
    version: string;
    notes: string;
    url: string;
  }>;
}

export interface VersionUpdate {
  version: string;
  priority: "urgent" | "normal" | "minor" | "low";
  announcement: {
    title: string;
    message: string;
  };
  compatibility: {
    breaking_changes: boolean;
    breaking_description?: string;
    migration_guide_url?: string;
  };
}

export interface FullUpdateInfo {
  latest_version: string;
  latest_stable_version?: string;
  new_versions: VersionUpdate[];
}

export interface MachineInfo {
  machineType: string;
  activeSecurity: boolean;
  licenseType: string;
}

interface GitHubRelease {
  tag_name: string;
  name: string | null;
  body: string | null;
  html_url: string;
  prerelease: boolean;
  draft: boolean;
}

/**
 * Update checks backed by the latest GitHub release of the Scripta repository.
 * Every method resolves to null (or "") on network or API failure; callers
 * treat that as "no update information".
 */
export class UpdateService {
  /**
   * Compare two version strings
   * @returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal
   */
  compareVersions(version1: string, version2: string): number {
    const v1 = version1.split(".");
    const v2 = version2.split(".");

    for (let i = 0; i < v1.length || i < v2.length; i++) {
      const n1 = parseInt(v1[i]) || 0;
      const n2 = parseInt(v2[i]) || 0;

      if (n1 > n2) {
        return 1;
      } else if (n1 < n2) {
        return -1;
      }
    }

    return 0;
  }

  /**
   * Download page for installations that update by replacing the app. Docker
   * and Kubernetes installs update by pulling a new image, so they get null.
   */
  getDownloadUrl(machineInfo: MachineInfo): string | null {
    if (
      machineInfo.machineType === "Docker" ||
      machineInfo.machineType === "Kubernetes"
    ) {
      return null;
    }
    return DOWNLOAD_BASE_URL;
  }

  private async fetchLatestRelease(): Promise<GitHubRelease | null> {
    try {
      const response = await fetch(LATEST_RELEASE_API, {
        headers: { Accept: "application/vnd.github+json" },
      });
      if (response.status !== 200) return null;
      const release = (await response.json()) as GitHubRelease;
      return release.draft || release.prerelease ? null : release;
    } catch (error) {
      console.error("Failed to fetch the latest release:", error);
      return null;
    }
  }

  private static versionOf(release: GitHubRelease): string {
    return release.tag_name.replace(/^v/i, "");
  }

  async getUpdateSummary(
    currentVersion: string,
    _machineInfo: MachineInfo,
  ): Promise<UpdateSummary | null> {
    const release = await this.fetchLatestRelease();
    if (!release) return null;
    const latest = UpdateService.versionOf(release);
    return {
      latest_version:
        this.compareVersions(latest, currentVersion) > 0 ? latest : null,
      latest_stable_version: latest,
      max_priority: "normal",
      any_breaking: false,
    };
  }

  async getFullUpdateInfo(
    currentVersion: string,
    _machineInfo: MachineInfo,
  ): Promise<FullUpdateInfo | null> {
    const release = await this.fetchLatestRelease();
    if (!release) return null;
    const latest = UpdateService.versionOf(release);
    const isNewer = this.compareVersions(latest, currentVersion) > 0;
    return {
      latest_version: latest,
      latest_stable_version: latest,
      new_versions: isNewer
        ? [
            {
              version: latest,
              priority: "normal",
              announcement: {
                title: release.name || `${APP_NAME} ${latest}`,
                message: release.body ?? "",
              },
              compatibility: {
                breaking_changes: false,
                migration_guide_url: release.html_url,
              },
            },
          ]
        : [],
    };
  }

  /** Latest released version number, or "" when it cannot be determined. */
  async getCurrentVersionFromGitHub(): Promise<string> {
    const release = await this.fetchLatestRelease();
    return release ? UpdateService.versionOf(release) : "";
  }
}

export const updateService = new UpdateService();
