export type Section = '人文' | '自然';
export interface Point { latitude: number; longitude: number; coordinateSystem: 'WGS84' | 'BD09' }
export interface Query { center: Point; section: Section; category: '全部'|'历史'|'美食'|'自然景区'; radiusMeters?: 1000; refresh?: boolean }
export interface Rank { board: string; position: number }
export interface Place {
  id: string; name: string; address: string; category: string; location: Point; distanceMeters: number;
  rankingRaw: unknown; rank: Rank|null; heat7d: number|null;
  score: number|null; scoreSource: 'rank_heat'|'rank'|'heat'|'none';
}
export interface Nearby {
  items: Place[]; center: Point; radiusMeters: 1000; source: 'baidu';
  fetchedAt: number; expiresAt: number; selectedRankBoard: string|null;
}
export class ApiError extends Error {
  constructor(public code: string, public status: number, public retryAfter?: number) { super(code); }
}
export interface Upstream {
  configured: boolean;
  convert(point: Point, signal: AbortSignal): Promise<Point>;
  page(center: Point, query: Query, page: number, signal: AbortSignal): Promise<{items: unknown[]; total?: number}>;
}
