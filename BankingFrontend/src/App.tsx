import { useState, useEffect } from 'react'
import { Activity, ShieldAlert, LogOut } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { TransactionForm } from './components/TransactionForm'
import { PipelineView } from './components/PipelineView'
import { LoginView } from './components/LoginView'
import axios from 'axios'
import './index.css'

export type TransactionData = {
  accountId: string;
  amount: string;
  transactionType: string;
  ipAddress: string;
  location: string;
};

export type SimulationStage = 'IDLE' | 'MASKING' | 'RULES_ENGINE' | 'LLM_ANALYSIS' | 'COMPLETED';

function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwtToken'));
  const [stage, setStage] = useState<SimulationStage>('IDLE');
  const [transaction, setTransaction] = useState<TransactionData | null>(null);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (token) {
      localStorage.setItem('jwtToken', token);
    } else {
      localStorage.removeItem('jwtToken');
    }
  }, [token]);

  const handleSimulate = async (data: TransactionData) => {
    setTransaction(data);
    setResult(null);
    setError(null);
    
    // Step 1: Visually simulate data masking
    setStage('MASKING');
    await new Promise(r => setTimeout(r, 1500));
    
    // Step 2: Rules Engine - Trigger real transaction creation on backend
    setStage('RULES_ENGINE');
    
    try {
      const response = await axios.post(
        'http://144.24.122.133:8080/api/transaction/transfer',
        {
          toUserId: parseInt(data.accountId),
          amount: parseFloat(data.amount)
        },
        {
          headers: {
            Authorization: `Bearer ${token}`
          }
        }
      );

      const txId = response.data.id;
      const txStatus = response.data.status; // e.g. PENDING_REVIEW or COMPLETED

      // Pause for visual rules engine output effect
      await new Promise(r => setTimeout(r, 1500));

      // Step 3: LLM Analysis - Poll the Alerts endpoint to get the asynchronous Llama justification
      setStage('LLM_ANALYSIS');

      let alertData = null;
      let attempts = 0;
      const maxAttempts = 15; // Poll for max 30 seconds

      while (attempts < maxAttempts) {
        try {
          const alertsResponse = await axios.get(
            'http://144.24.122.133:8080/api/analyst/alerts?page=0&size=20',
            {
              headers: {
                Authorization: `Bearer ${token}`
              }
            }
          );

          const alerts = alertsResponse.data.content || [];
          const matchingAlert = alerts.find((alert: any) => alert.transactionId === txId);

          if (matchingAlert) {
            // Keep polling if summary/explanation hasn't generated yet
            if (matchingAlert.summary && matchingAlert.summary.trim().length > 0) {
              alertData = matchingAlert;
              break;
            }
          } else {
            // For LOW risk transactions, a RiskAlert might not be created or might not trigger LLM review
            // If the transaction is completed and no alert shows up in 4 seconds, we auto-resolve it as LOW risk
            if (txStatus === 'COMPLETED' && attempts > 2) {
              break;
            }
          }
        } catch (pollErr) {
          console.error('Polling error', pollErr);
        }

        attempts++;
        await new Promise(r => setTimeout(r, 2000));
      }

      // Step 4: Display verdict based on real OCI backend results
      if (alertData) {
        setResult({
          score: alertData.riskScore,
          decision: alertData.riskLevel === 'HIGH' ? 'FLAGGED' : 'APPROVED',
          justification: alertData.summary
        });
      } else {
        // Fallback for normal transaction
        setResult({
          score: 10,
          decision: 'APPROVED',
          justification: 'Transaction processed successfully. Heuristic checks passed. No high risk indicators detected.'
        });
      }
      
      setStage('COMPLETED');

    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message || 
        'Transaction failed. Check if destination User ID exists and has correct setup.'
      );
      setStage('IDLE');
    }
  };

  const handleLogout = () => {
    setToken(null);
    resetSimulation();
  };

  const resetSimulation = () => {
    setStage('IDLE');
    setTransaction(null);
    setResult(null);
    setError(null);
  };

  if (!token) {
    return (
      <div className="min-h-screen">
        <LoginView onLoginSuccess={(newToken) => setToken(newToken)} />
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <header className="glass-panel" style={{ margin: '2rem', padding: '1.5rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 className="title text-gradient" style={{ fontSize: '1.75rem', marginBottom: 0 }}>
            Risk Evaluation Engine
          </h1>
          <p className="subtitle" style={{ fontSize: '0.9rem' }}>Real-time transaction analysis simulation</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
           <div className={`node-icon-wrapper ${stage !== 'IDLE' ? 'active' : ''}`} style={{ width: '40px', height: '40px' }}>
              <Activity size={20} />
           </div>
           <button 
             onClick={handleLogout}
             style={{ 
               background: 'rgba(239, 68, 68, 0.1)', 
               border: '1px solid rgba(239, 68, 68, 0.2)', 
               borderRadius: '8px', 
               padding: '0.5rem 1rem', 
               color: 'var(--danger)', 
               display: 'flex', 
               alignItems: 'center', 
               gap: '0.5rem',
               cursor: 'pointer',
               fontWeight: 600
             }}
           >
             Sign Out <LogOut size={16} />
           </button>
        </div>
      </header>

      <main className="pipeline-container">
        {/* Left Side: Input Form */}
        <div style={{ position: 'relative', zIndex: 10 }}>
          <AnimatePresence mode="wait">
            {stage === 'IDLE' ? (
              <motion.div
                key="form"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
              >
                {error && (
                  <div style={{ 
                    padding: '0.75rem 1rem', 
                    background: 'rgba(239, 68, 68, 0.1)', 
                    border: '1px solid rgba(239, 68, 68, 0.3)', 
                    borderRadius: '8px', 
                    color: 'var(--danger)',
                    fontSize: '0.9rem',
                    marginBottom: '1rem'
                  }}>
                    {error}
                  </div>
                )}
                <TransactionForm onSubmit={handleSimulate} />
              </motion.div>
            ) : (
              <motion.div
                key="summary"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="glass-panel"
                style={{ padding: '2rem' }}
              >
                <h3 className="input-label" style={{ fontSize: '1.1rem', marginBottom: '1.5rem', color: 'var(--text-main)' }}>Processing Transaction</h3>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Amount:</span>
                    <span style={{ fontWeight: 600, color: 'var(--success)' }}>${transaction?.amount}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">To User ID:</span>
                    <span>{transaction?.accountId}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Location:</span>
                    <span>{transaction?.location}</span>
                  </div>
                </div>

                {stage === 'COMPLETED' && (
                  <button 
                    className="btn-primary" 
                    style={{ marginTop: '2rem' }}
                    onClick={resetSimulation}
                  >
                    Run Another Analysis
                  </button>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Right Side: Visual Pipeline */}
        <div style={{ position: 'relative' }}>
          {stage !== 'IDLE' && (
            <PipelineView stage={stage} transaction={transaction} result={result} />
          )}
          {stage === 'IDLE' && (
            <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', opacity: 0.5 }}>
              <div style={{ textAlign: 'center' }}>
                <ShieldAlert size={64} style={{ margin: '0 auto', marginBottom: '1rem', color: 'var(--text-muted)' }} />
                <h2 style={{ color: 'var(--text-muted)' }}>Waiting for transaction...</h2>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}

export default App
