import type { ReactNode } from "react";
import "@app/components/shared/config/SettingsRow.css";

export interface SettingsRowProps {
  /** The row's anchor: settings search links to `/settings/{page}#{id}`. */
  id: string;
  label: ReactNode;
  /** One line under the label saying what the setting does. */
  description?: ReactNode;
  /** Lets the control name itself with `aria-labelledby`. */
  labelId?: string;
  /** The control, drawn at the right-hand end. */
  children: ReactNode;
}

/**
 * One setting: its name and what it does on the left, the control on the right.
 * Rows go inside a `.settings-group`, which draws the card and the lines
 * between rows.
 */
export function SettingsRow({
  id,
  label,
  description,
  labelId,
  children,
}: SettingsRowProps) {
  return (
    <div id={id} className="settings-row">
      <div className="settings-row__text">
        <span id={labelId} className="settings-row__label">
          {label}
        </span>
        {description && (
          <span className="settings-row__description">{description}</span>
        )}
      </div>
      <div className="settings-row__control">{children}</div>
    </div>
  );
}
