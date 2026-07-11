import { useState, useEffect } from 'react';

import { ShieldAlert, Check, X, Loader2 } from 'lucide-react';
import axios from 'axios';

interface Props {
  token: string;
}

export function AlertsQueue({ token }: Props) {
  const [alerts, setAlerts] = useState<any[]>([]);

  const [processingId, setProcessingId] = useState<number | null>(null);

  const fetchAlerts = async () => {
    try {
      const response = await axios.get('http://144.24.122.133:8080/api/analyst/alerts?page=0&size=10', {
        headers: { Authorization: `Bearer ${token}` }
      });
      // Filter for alerts that need review (PENDING/UNDER_REVIEW)
      const pendingAlerts = response.data.content.filter(
        (a: any) => a.riskStatus === 'CREATED' || a.riskStatus === 'UNDER_REVIEW'
      );
      setAlerts(pendingAlerts);
    } catch (err) {
      console.error('Failed to fetch alerts', err);
    }
  };

  // Poll every 5 seconds
  useEffect(() => {
    fetchAlerts();
    const interval = setInterval(fetchAlerts, 5000);
    return () => clearInterval(interval);
  }, [token]);

  const handleResolve = async (alertId: number, action: 'APPROVE' | 'REJECT') => {
    setProcessingId(alertId);
    try {
      await axios.post(
        `http://144.24.122.133:8080/api/analyst/alerts/${alertId}/resolve`,
        {
          action: action,
          notes: `Analyst decision: ${action} via Control Center`
        },
        {
          headers: { Authorization: `Bearer ${token}` }
        }
      );
      // Re-fetch immediately to update UI
      fetchAlerts();
    } catch (err) {
      console.error('Failed to resolve alert', err);
      alert('Failed to resolve alert. Please try again.');
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="glass-panel" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '1.25rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className="title" style={{ fontSize: '1.1rem', margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <ShieldAlert size={18} color="var(--danger)" />
          Compliance Alerts Queue
        </h2>
        <span style={{ background: 'var(--danger-bg)', color: 'var(--danger)', padding: '0.1rem 0.5rem', borderRadius: '1rem', fontSize: '0.8rem', fontWeight: 600 }}>
          {alerts.length} Pending
        </span>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        {alerts.length === 0 ? (
          <div style={{ padding: '3rem 2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <p>No pending alerts requiring analyst review.</p>
          </div>
        ) : (
          alerts.map(alert => (
            <div key={alert.id} className="alert-item">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>TX-ID: {alert.transactionId}</span>
                <span style={{ color: 'var(--danger)', fontWeight: 600, fontSize: '0.9rem' }}>Score: {alert.riskScore}</span>
              </div>
              
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem', background: '#f1f5f9', padding: '0.75rem', borderRadius: '4px' }}>
                {alert.summary || 'Awaiting LLM justification...'}
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button 
                  className="btn-outline" 
                  style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.25rem', color: 'var(--success)', borderColor: 'var(--success)' }}
                  onClick={() => handleResolve(alert.id, 'APPROVE')}
                  disabled={processingId === alert.id}
                >
                  {processingId === alert.id ? <Loader2 size={16} className="is-processing" /> : <Check size={16} />}
                  Approve
                </button>
                <button 
                  className="btn-outline"
                  style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.25rem', color: 'var(--danger)', borderColor: 'var(--danger)' }}
                  onClick={() => handleResolve(alert.id, 'REJECT')}
                  disabled={processingId === alert.id}
                >
                  {processingId === alert.id ? <Loader2 size={16} className="is-processing" /> : <X size={16} />}
                  Reject
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
