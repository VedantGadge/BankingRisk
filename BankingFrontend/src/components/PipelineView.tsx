import { motion } from 'framer-motion';
import { Shield, BrainCircuit, FileSearch, CheckCircle2, AlertTriangle, Fingerprint } from 'lucide-react';
import type { SimulationStage, TransactionData } from '../App';

interface Props {
  stage: SimulationStage;
  transaction: TransactionData | null;
  result: any;
}

export function PipelineView({ stage, transaction, result }: Props) {
  const getStageIndex = (s: SimulationStage) => {
    switch (s) {
      case 'IDLE': return 0;
      case 'MASKING': return 1;
      case 'RULES_ENGINE': return 2;
      case 'LLM_ANALYSIS': return 3;
      case 'COMPLETED': return 4;
      default: return 0;
    }
  };

  const currentIdx = getStageIndex(stage);

  return (
    <div style={{ paddingLeft: '1rem', position: 'relative' }}>
      <div className="node-connector" />

      {/* Node 1: PII Masking */}
      <div className="pipeline-node">
        <div className="node-header">
          <div className={`node-icon-wrapper ${currentIdx >= 1 ? 'active' : ''} ${stage === 'MASKING' ? 'is-processing' : ''}`}>
            <Fingerprint size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Data Masking & Anonymization</h3>
            <p className="text-muted" style={{ fontSize: '0.9rem' }}>Removing PII before analysis</p>
          </div>
        </div>
        
        {currentIdx >= 1 && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }} 
            animate={{ opacity: 1, y: 0 }}
            className="node-content"
          >
            <div style={{ marginBottom: '1rem' }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Raw Data:</div>
              <div>
                <span className="data-tag">Acct: {transaction?.accountId}</span>
                <span className="data-tag">IP: {transaction?.ipAddress}</span>
                <span className="data-tag">Loc: {transaction?.location}</span>
              </div>
            </div>
            
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: currentIdx > 1 || stage === 'RULES_ENGINE' || stage === 'MASKING' ? 1 : 0 }}
              transition={{ delay: 0.8 }}
            >
              <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Masked Data:</div>
              <div>
                <span className="data-tag masked">Acct: ***-**392-XX</span>
                <span className="data-tag masked">IP: 192.168.*.*</span>
                <span className="data-tag">Loc: {transaction?.location}</span>
              </div>
            </motion.div>
          </motion.div>
        )}
      </div>

      {/* Node 2: Rules Engine */}
      <div className="pipeline-node">
        <div className="node-header">
          <div className={`node-icon-wrapper ${currentIdx >= 2 ? 'active' : ''} ${stage === 'RULES_ENGINE' ? 'is-processing' : ''}`}>
            <FileSearch size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Heuristic Rules Engine</h3>
            <p className="text-muted" style={{ fontSize: '0.9rem' }}>Evaluating velocity and hard limits</p>
          </div>
        </div>

        {currentIdx >= 2 && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }} 
            animate={{ opacity: 1, y: 0 }}
            className="node-content code-block"
          >
            <div><span style={{ color: '#60a5fa' }}>Evaluating</span>: Velocity Check (24h)</div>
            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }} 
              transition={{ delay: 0.5 }}
            >
              <div><span style={{ color: 'var(--success)' }}>[PASS]</span> Transaction count within normal limits</div>
            </motion.div>

            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }} 
              transition={{ delay: 1.0 }}
              style={{ marginTop: '0.5rem' }}
            >
              <div><span style={{ color: '#60a5fa' }}>Evaluating</span>: Amount Threshold Check</div>
              <div>
                {parseInt(transaction?.amount || '0') > 10000 ? (
                  <span style={{ color: 'var(--warning)' }}>[WARN] Large amount detected (${transaction?.amount})</span>
                ) : (
                  <span style={{ color: 'var(--success)' }}>[PASS] Amount within standard deviation</span>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </div>

      {/* Node 3: LLM Analysis */}
      <div className="pipeline-node">
        <div className="node-header">
          <div className={`node-icon-wrapper ${currentIdx >= 3 ? 'active' : ''} ${stage === 'LLM_ANALYSIS' ? 'is-processing' : ''}`}>
            <BrainCircuit size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Llama 3 Risk Analysis</h3>
            <p className="text-muted" style={{ fontSize: '0.9rem' }}>Generating contextual risk justification</p>
          </div>
        </div>

        {currentIdx >= 3 && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }} 
            animate={{ opacity: 1, y: 0 }}
            className="node-content"
          >
            {stage === 'LLM_ANALYSIS' ? (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <span style={{ width: '8px', height: '8px', background: 'var(--primary)', borderRadius: '50%', display: 'inline-block' }} className="is-processing" />
                <span style={{ color: 'var(--text-muted)' }}>LLM is analyzing the masked context...</span>
              </div>
            ) : (
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Final Score</span>
                  <span style={{ fontWeight: 800, fontSize: '1.2rem', color: result?.score > 50 ? 'var(--danger)' : 'var(--success)' }}>
                    {result?.score} / 100
                  </span>
                </div>
                <div style={{ padding: '1rem', background: 'rgba(0,0,0,0.2)', borderRadius: '8px', borderLeft: `3px solid ${result?.score > 50 ? 'var(--warning)' : 'var(--success)'}` }}>
                  <p style={{ fontStyle: 'italic', fontSize: '0.95rem', color: '#e2e8f0' }}>"{result?.justification}"</p>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </div>

      {/* Node 4: Final Verdict */}
      <div className="pipeline-node">
        <div className="node-header">
          <div className={`node-icon-wrapper ${currentIdx >= 4 ? (result?.decision === 'FLAGGED' ? 'danger' : 'success') : ''}`}>
            <Shield size={24} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Final Verdict</h3>
          </div>
        </div>
        
        {currentIdx >= 4 && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.9 }} 
            animate={{ opacity: 1, scale: 1 }}
            className="node-content"
            style={{ 
              background: result?.decision === 'FLAGGED' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
              borderColor: result?.decision === 'FLAGGED' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(16, 185, 129, 0.3)',
              display: 'flex',
              alignItems: 'center',
              gap: '1rem'
            }}
          >
            {result?.decision === 'FLAGGED' ? (
              <AlertTriangle size={32} color="var(--danger)" />
            ) : (
              <CheckCircle2 size={32} color="var(--success)" />
            )}
            <div>
              <h2 style={{ 
                color: result?.decision === 'FLAGGED' ? 'var(--danger)' : 'var(--success)',
                margin: 0,
                fontSize: '1.5rem',
                fontWeight: 800
              }}>
                {result?.decision}
              </h2>
              <p style={{ color: 'var(--text-main)', margin: 0, opacity: 0.8 }}>
                {result?.decision === 'FLAGGED' ? 'Transaction flagged for manual review.' : 'Transaction processed successfully.'}
              </p>
            </div>
          </motion.div>
        )}
      </div>
    </div>
  );
}
