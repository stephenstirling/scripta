import { Icon } from "@app/ui/Icon";
import "@app/components/shared/BrandMark.css";

interface BrandMarkProps {
  /** Height of the mark (CSS length). */
  height?: string;
  className?: string;
}

/**
 * The Scripta logo mark, rendered inline (not as an <img>) so CSS can colour
 * and animate its parts. When an ancestor marked `[data-brandmark-morph]` is
 * hovered / focused / open (`.is-open`), the mark cross-fades into a downward
 * chevron, a "this opens a menu" affordance. See BrandMark.css.
 */
export function BrandMark({ height = "1.6rem", className }: BrandMarkProps) {
  return (
    <Icon
      name="scripta-mark"
      size={height}
      className={`sui-brandmark${className ? ` ${className}` : ""}`}
      title="Scripta"
    />
  );
}
