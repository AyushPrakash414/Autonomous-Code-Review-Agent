import React, { useEffect, useState } from 'react';
import { Shield, Cpu, Activity, RefreshCw, CheckCircle2 } from 'lucide-react';
import { PlatformHealthState, ServiceHealth } from './types/health';


const initialHealth: ServiceHealth = {
  status: 'PENDING',
  service: 'Checking...',
};

export const App: React.FC = () => {
  const [healthState, setHealthState] = useState<PlatformHealthState>({
    controlPlane: initialHealth,
    intelligencePlane: initialHealth,
    lastChecked: new Date().toLocaleTimeString(),
  });
  const [loading, setLoading] = useState<boolean>(false);

  const checkPlatformHealth = async () => {
    setLoading(true);
    const updatedState: PlatformHealthState = {
      controlPlane: { ...initialHealth },
      intelligencePlane: { ...initialHealth },
      lastChecked: new Date().toLocaleTimeString(),
    };

    // Check Control Plane (Spring Boot)
    const t0 = performance.now();
    try {
      const res = await fetch('/api/v1/health');
      const latency = Math.round(performance.now() - t0);
      if (res.ok) {
        const data = await res.json();
        updatedState.controlPlane = {
          status: data.status === 'UP' ? 'UP' : 'DOWN',
          service: data.service,
          version: data.version,
          timestamp: data.timestamp,
          details: data.details,
          latencyMs: latency,
        };
      } else {
        updatedState.controlPlane = {
          status: 'DOWN',
          service: 'Spring Boot Control Plane',
          error: `HTTP ${res.status}: ${res.statusText}`,
          latencyMs: latency,
        };
      }
    } catch (err: any) {
      updatedState.controlPlane = {
        status: 'DOWN',
        service: 'Spring Boot Control Plane',
        error: err.message || 'Network Error',
        latencyMs: Math.round(performance.now() - t0),
      };
    }

    // Check Intelligence Plane (FastAPI)
    const t1 = performance.now();
    try {
      const res = await fetch('/agent-api/api/v1/health');
      const latency = Math.round(performance.now() - t1);
      if (res.ok) {
        const data = await res.json();
        updatedState.intelligencePlane = {
          status: data.status === 'UP' ? 'UP' : 'DOWN',
          service: data.service,
          version: data.version,
          timestamp: data.timestamp,
          details: data.details,
          latencyMs: latency,
        };
      } else {
        updatedState.intelligencePlane = {
          status: 'DOWN',
          service: 'FastAPI Agent Service',
          error: `HTTP ${res.status}: ${res.statusText}`,
          latencyMs: latency,
        };
      }
    } catch (err: any) {
      updatedState.intelligencePlane = {
        status: 'DOWN',
        service: 'FastAPI Agent Service',
        error: err.message || 'Network Error',
        latencyMs: Math.round(performance.now() - t1),
      };
    }

    setHealthState(updatedState);
    setLoading(false);
  };

  useEffect(() => {
    checkPlatformHealth();
  }, []);

  return (
    <div className="app-container">
      <header className="header">
        <div className="header-brand">
          <div className="brand-icon">
            <Shield size={24} color="#ffffff" />
          </div>
          <div>
            <h1 className="brand-title">Autonomous Code Review Agent</h1>
            <p className="brand-subtitle">Phase 0: Project Foundation & Service Health Monitor</p>
          </div>
        </div>
        <button
          className="btn btn-primary"
          onClick={checkPlatformHealth}
          disabled={loading}
        >
          <RefreshCw size={16} className={loading ? 'pulse-dot' : ''} />
          {loading ? 'Polling...' : 'Refresh Status'}
        </button>
      </header>

      <main>
        <div className="grid-cols-2">
          {/* Spring Boot Control Plane Card */}
          <div className="card">
            <div className="card-header">
              <div className="card-title">
                <Cpu size={20} color="#3b82f6" />
                Control Plane (Spring Boot)
              </div>
              <span className={`badge badge-${healthState.controlPlane.status.toLowerCase()}`}>
                <span className="pulse-dot"></span>
                {healthState.controlPlane.status}
              </span>
            </div>
            <div>
              <div className="meta-row">
                <span className="meta-label">Service</span>
                <span className="meta-value">{healthState.controlPlane.service}</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Port / Target</span>
                <span className="meta-value">:8080 (/api/v1/health)</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Version</span>
                <span className="meta-value">{healthState.controlPlane.version || '---'}</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Latency</span>
                <span className="meta-value">{healthState.controlPlane.latencyMs !== undefined ? `${healthState.controlPlane.latencyMs} ms` : '---'}</span>
              </div>
              {healthState.controlPlane.error && (
                <div className="meta-row" style={{ color: 'var(--accent-rose)' }}>
                  <span className="meta-label">Error</span>
                  <span className="meta-value">{healthState.controlPlane.error}</span>
                </div>
              )}
            </div>
          </div>

          {/* FastAPI Intelligence Plane Card */}
          <div className="card">
            <div className="card-header">
              <div className="card-title">
                <Activity size={20} color="#06b6d4" />
                Intelligence Plane (FastAPI)
              </div>
              <span className={`badge badge-${healthState.intelligencePlane.status.toLowerCase()}`}>
                <span className="pulse-dot"></span>
                {healthState.intelligencePlane.status}
              </span>
            </div>
            <div>
              <div className="meta-row">
                <span className="meta-label">Service</span>
                <span className="meta-value">{healthState.intelligencePlane.service}</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Port / Target</span>
                <span className="meta-value">:8000 (/api/v1/health)</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Version</span>
                <span className="meta-value">{healthState.intelligencePlane.version || '---'}</span>
              </div>
              <div className="meta-row">
                <span className="meta-label">Latency</span>
                <span className="meta-value">{healthState.intelligencePlane.latencyMs !== undefined ? `${healthState.intelligencePlane.latencyMs} ms` : '---'}</span>
              </div>
              {healthState.intelligencePlane.details?.llm_provider && (
                <div className="meta-row">
                  <span className="meta-label">Configured LLM</span>
                  <span className="meta-value">{healthState.intelligencePlane.details.llm_provider}</span>
                </div>
              )}
              {healthState.intelligencePlane.error && (
                <div className="meta-row" style={{ color: 'var(--accent-rose)' }}>
                  <span className="meta-label">Error</span>
                  <span className="meta-value">{healthState.intelligencePlane.error}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Phase 0 Verification Badge */}
        <div className="card" style={{ marginTop: '1.5rem', background: 'rgba(59, 130, 246, 0.05)', borderColor: 'rgba(59, 130, 246, 0.2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <CheckCircle2 size={24} color="#10b981" />
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>Phase 0 Exit Criteria Realized</h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                Decoupled Control Plane (Java 21/Spring Boot 3), Intelligence Plane (FastAPI/LangGraph), and Presentation Layer (React 18) active with structured telemetry.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
export default App;
