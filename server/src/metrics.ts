import type { Rank, Place } from './types.js';
// Unknown provider formats remain raw; no guesses based on result order or ratings.
export function parseRank(raw: unknown): Rank|null {
  if (typeof raw !== 'string') return null;
  const match = raw.trim().match(/^(.+?)\s*第\s*([1-9]\d*)\s*名$/u);
  if (!match) return null;
  const position = Number(match[2]);
  return Number.isSafeInteger(position) ? {board: match[1].trim(), position} : null;
}
export function heatValue(value: unknown): number|null {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null;
}
export function scorePlaces(items: Place[]): string|null {
  const counts = new Map<string,number>();
  for (const p of items) if (p.rank) counts.set(p.rank.board,(counts.get(p.rank.board)??0)+1);
  const board = [...counts.keys()].sort((a,b)=>(counts.get(b)!-counts.get(a)!) || (a<b?-1:a>b?1:0))[0]??null;
  const maximum = Math.max(0,...items.map(p=>p.heat7d??0));
  for (const p of items) {
    const r = p.rank?.board === board ? 1/p.rank.position : null;
    const h = p.heat7d === null ? null : maximum === 0 ? 0 : Math.log1p(p.heat7d)/Math.log1p(maximum);
    p.score = r===null ? h : h===null ? r : (r+h)/2;
    p.scoreSource = r===null ? h===null?'none':'heat' : h===null?'rank':'rank_heat';
  }
  return board;
}
export function distanceMeters(a: {latitude:number;longitude:number},b: {latitude:number;longitude:number}): number {
  const rad=Math.PI/180, dLat=(b.latitude-a.latitude)*rad, dLon=(b.longitude-a.longitude)*rad;
  const h=Math.sin(dLat/2)**2+Math.cos(a.latitude*rad)*Math.cos(b.latitude*rad)*Math.sin(dLon/2)**2;
  return 6371008.8*2*Math.asin(Math.sqrt(Math.min(1,h)));
}
export function keywords(q: {section:string;category:string}): string {
  if(q.section==='自然')return '自然风光$风景区$公园';
  if(q.category==='历史')return '文物古迹$历史建筑$博物馆';
  if(q.category==='美食')return '美食';
  return '文物古迹$历史建筑$博物馆$美食';
}
