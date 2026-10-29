import React from "react";

interface ToolIconProps {
  icon: React.ReactNode;
  opacity?: number;
  color?: string;
  marginRight?: string;
  /** The registry icons are drawn at 1.5rem; 0.8 fits them to the dense rows. */
  scale?: number;
}

/**
 * Shared icon component for consistent tool icon styling across the application.
 * Uses the same visual pattern as ToolButton: scaled to 0.8, centered transform, consistent spacing.
 */
export const ToolIcon: React.FC<ToolIconProps> = ({
  icon,
  opacity = 1,
  color = "var(--c-text)",
  marginRight = "0.5rem",
  scale = 0.8,
}) => {
  return (
    <div
      className="tool-button-icon"
      style={{
        color,
        marginRight,
        transform: `scale(${scale})`,
        transformOrigin: "center",
        opacity,
      }}
    >
      {icon}
    </div>
  );
};
