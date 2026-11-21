import { useEffect, useMemo } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import type { Space } from '../api/schemas';

// Standard OpenStreetMap tiles (free, attribution required). styles.css mutes their colours (and
// inverts them in dark mode) so the price pins stand out.
const TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

const TUNIS: [number, number] = [36.8065, 10.1815];

export function hasLocation(s: Pick<Space, 'coordinateX' | 'coordinateY'>): boolean {
  return !(s.coordinateX === 0 && s.coordinateY === 0);
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);
}

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap();
  // Serialised so the map only re-fits when the coordinates change, not on every new array.
  const serialized = JSON.stringify(points);
  useEffect(() => {
    // Re-measure first: the container may have been laid out after the map was created.
    map.invalidateSize();
    const pts: [number, number][] = JSON.parse(serialized);
    const first = pts[0];
    if (pts.length === 1 && first) map.setView(first, 14);
    else if (pts.length > 1) map.fitBounds(L.latLngBounds(pts), { padding: [48, 48], maxZoom: 14 });
  }, [map, serialized]);
  return null;
}

/** Keeps the map's idea of its size in step with its container (sticky panels, responsive layouts). */
function TrackSize() {
  const map = useMap();
  useEffect(() => {
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(map.getContainer());
    return () => observer.disconnect();
  }, [map]);
  return null;
}

interface SpaceMapProps {
  spaces: Space[];
  labels?: Record<number, string>;
  activeId?: number | null;
  onSelect?: (id: number) => void;
  onHover?: (id: number | null) => void;
  className?: string;
  label: string;
}

/** Spaces as price pins on a map. */
export function SpaceMap({ spaces, labels = {}, activeId, onSelect, onHover, className = '', label }: SpaceMapProps) {
  const located = spaces.filter(hasLocation);
  const points = located.map((s): [number, number] => [s.coordinateX, s.coordinateY]);

  return (
    <div className={`map ${className}`} role="region" aria-label={label}>
      <MapContainer center={points[0] ?? TUNIS} zoom={12} scrollWheelZoom={false} attributionControl>
        <TileLayer url={TILES} attribution={ATTRIBUTION} />
        <TrackSize />
        <FitBounds points={points} />
        {located.map((s) => (
          <PricePin
            key={s.id}
            space={s}
            text={labels[s.id] ?? s.name ?? 'Space'}
            active={s.id === activeId}
            onSelect={onSelect}
            onHover={onHover}
          />
        ))}
      </MapContainer>
    </div>
  );
}

function PricePin({ space, text, active, onSelect, onHover }: {
  space: Space;
  text: string;
  active: boolean;
  onSelect?: (id: number) => void;
  onHover?: (id: number | null) => void;
}) {
  const icon = useMemo(
    () => L.divIcon({ className: '', html: `<div class="pin" data-active="${active}">${escapeHtml(text)}</div>`, iconSize: [0, 0] }),
    [text, active],
  );
  return (
    <Marker
      position={[space.coordinateX, space.coordinateY]}
      icon={icon}
      title={space.name ?? undefined}
      keyboard
      zIndexOffset={active ? 1000 : 0}
      eventHandlers={{
        click: () => onSelect?.(space.id),
        mouseover: () => onHover?.(space.id),
        mouseout: () => onHover?.(null),
      }}
    />
  );
}

function ClickToPick({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick(e.latlng.lat, e.latlng.lng) });
  return null;
}

/** A map where clicking sets a location. */
export function LocationPicker({ value, onChange, label }: {
  value: [number, number] | null;
  onChange: (lat: number, lng: number) => void;
  label: string;
}) {
  const icon = useMemo(() => L.divIcon({ className: '', html: '<div class="pin-dot"></div>', iconSize: [0, 0] }), []);
  return (
    <div className="map picker-map" role="region" aria-label={label}>
      <MapContainer center={value ?? TUNIS} zoom={12} scrollWheelZoom={false}>
        <TileLayer url={TILES} attribution={ATTRIBUTION} />
        <ClickToPick onPick={onChange} />
        {value && <Marker position={value} icon={icon} />}
      </MapContainer>
    </div>
  );
}
