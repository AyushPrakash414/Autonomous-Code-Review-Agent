export interface ServiceHealth {
  status: 'UP' | 'DOWN' | 'PENDING';
  service: string;
  version?: string;
  timestamp?: string;
  details?: Record<string, any>;
  latencyMs?: number;
  error?: string;
}

export interface PlatformHealthState {
  controlPlane: ServiceHealth;
  intelligencePlane: ServiceHealth;
  lastChecked: string;
}
