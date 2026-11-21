import type { CSSProperties, ReactNode } from 'react';
import { spaceHue } from '../lib/format';
import { initials } from './Layout';

interface CoverProps {
  spaceId: number;
  name: string | null;
  className?: string;
  tag?: ReactNode;
}

/** Generated artwork for a space (the API has no photos yet): a colour field unique to the space. */
export function Cover({ spaceId, name, className = '', tag }: CoverProps) {
  const style = { '--h': spaceHue(spaceId) } as CSSProperties;
  return (
    <div className={`cover ${className}`} style={style} aria-hidden="true">
      <span className="cover-initials">{initials(name)}</span>
      {tag && <span className="cover-tag">{tag}</span>}
    </div>
  );
}
