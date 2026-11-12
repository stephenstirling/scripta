import { useState, type ReactNode } from "react";
import { Icon } from "@app/ui/Icon";
import "@app/components/shared/config/SettingsCard.css";

export interface SettingsCardProps {
  /** The card's `#slug`, and the id deep links and search results land on. */
  id: string;
  title: string;
  /** One line under the title saying what the group is for. */
  description?: ReactNode;
  /** A tier or maturity marker the retired nav row used to carry. */
  badge?: ReactNode;
  /**
   * Makes the card foldable and says whether it starts shut. Leave it unset for
   * an ordinary group, which always shows its settings.
   */
  defaultCollapsed?: boolean;
  /** Hold off mounting children until first opened, for expensive content. */
  lazy?: boolean;
  children: ReactNode;
}

/**
 * One titled group on a settings page. Only long reference lists (licences)
 * fold away; then the heading is a real button, so it is keyboard-reachable,
 * and the id sits on it so an anchor jump lands on the control that opens it.
 */
export function SettingsCard({
  id,
  title,
  description,
  badge,
  defaultCollapsed,
  lazy = false,
  children,
}: SettingsCardProps) {
  const foldable = defaultCollapsed !== undefined;
  const [open, setOpen] = useState(!defaultCollapsed);
  const [everOpened, setEverOpened] = useState(!defaultCollapsed);

  return (
    <section className="settings-card" data-open={open || undefined}>
      <div className="settings-card__head">
        <h2 className="settings-card__heading">
          {foldable ? (
            <button
              type="button"
              id={id}
              className="settings-card__toggle"
              aria-expanded={open}
              aria-controls={`${id}-panel`}
              onClick={() => {
                setOpen((wasOpen) => !wasOpen);
                setEverOpened(true);
              }}
            >
              <Icon
                name="chevron-down"
                size={16}
                className="settings-card__chevron"
              />
              <span className="settings-card__title">{title}</span>
            </button>
          ) : (
            <span id={id} className="settings-card__title" tabIndex={-1}>
              {title}
            </span>
          )}
          {badge}
        </h2>
        {description && (
          <p className="settings-card__description">{description}</p>
        )}
      </div>
      <div id={`${id}-panel`} className="settings-card__panel" hidden={!open}>
        {lazy && !everOpened ? null : children}
      </div>
    </section>
  );
}
