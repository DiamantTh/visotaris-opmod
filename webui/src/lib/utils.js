/** Gemeinsame Hilfsfunktionen für das Visotaris Web-UI */

/**
 * Formatiert einen snake_case Material-Key in Title Case.
 * @param {string} k  z.B. "diamond_sword"
 * @returns {string}  z.B. "Diamond Sword"
 */
export function fmtItem(k) {
  if (!k) return ''
  return k.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

/**
 * Formatiert eine Zahl als DE-Dezimalzahl mit 2 Nachkommastellen.
 * Gibt '–' zurück wenn Wert fehlt oder ≤ 0.
 */
export function fmt(v) {
  if (!v || v <= 0) return '–'
  return new Intl.NumberFormat('de-DE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(v)
}

/**
 * Formatiert eine Ganzzahl mit Tausender-Gruppierung.
 */
export function fmtInt(v) {
  if (v == null) return '0'
  return new Intl.NumberFormat('de-DE').format(v)
}

/**
 * Berechnet die CSS-Klasse für die Spanne (Spread) zwischen Kauf- und Verkaufspreis.
 */
export function spreadClass(buy, sell) {
  if (!buy || !sell || buy <= 0 || sell <= 0) return 'text-muted'
  const ratio = (buy - sell) / buy
  if (ratio < 0.05) return 'spread-low'
  if (ratio < 0.20) return 'spread-mid'
  return 'spread-high'
}
