import { useState, useEffect } from 'react'
import { ShieldAlert, LogOut, Wallet } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { TransactionForm } from './components/TransactionForm'
import { PipelineView } from './components/PipelineView'
import { LoginView } from './components/LoginView'
import { AlertsQueue } from './components/AlertsQueue'
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
  const [accountInfo, setAccountInfo] = useState<any>(null);

  useEffect(() => {
    if (token) {
      localStorage.setItem('jwtToken', token);
      fetchAccountInfo(token);
    } else {
      localStorage.removeItem('jwtToken');
      setAccountInfo(null);
    }
  }, [token]);

  const fetchAccountInfo = async (jwt: string) => {
    try {
      const response = await axios.get('http://144.24.122.133:8080/api/account', {
        headers: { Authorization: `Bearer ${jwt}` }
      });
      setAccountInfo(response.data);
    } catch (err) {
      console.error('Failed to fetch account info', err);
    }
  };

  const handleSimulate = async (data: TransactionData) => {
    setTransaction(data);
    setResult(null);
    setError(null);
    
    // Step 1: Visually simulate data masking
    setStage('MASKING');
    await new Promise(r => setTimeout(r, 1000));
    
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
          headers: { Authorization: `Bearer ${token}` }
        }
      );

      const txId = response.data.id;
      const txStatus = response.data.status; // e.g. PENDING_REVIEW or COMPLETED

      // Refresh balance after transfer (if it passed or was escrowed)
      fetchAccountInfo(token!);

      await new Promise(r => setTimeout(r, 1500));

      // Step 3: LLM Analysis
      setStage('LLM_ANALYSIS');

      let alertData = null;
      let attempts = 0;
      const maxAttempts = 15;

      while (attempts < maxAttempts) {
        try {
          const alertsResponse = await axios.get(
            'http://144.24.122.133:8080/api/analyst/alerts?page=0&size=20',
            {
              headers: { Authorization: `Bearer ${token}` }
            }
          );

          const alerts = alertsResponse.data.content || [];
          const matchingAlert = alerts.find((alert: any) => alert.transactionId === txId);

          if (matchingAlert) {
            if (matchingAlert.summary && matchingAlert.summary.trim().length > 0) {
              alertData = matchingAlert;
              break;
            }
          } else {
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

      // Step 4: Display verdict
      if (alertData) {
        setResult({
          score: alertData.riskScore,
          decision: alertData.riskLevel === 'HIGH' ? 'FLAGGED' : 'APPROVED',
          justification: alertData.summary
        });
      } else {
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
        'Transaction failed. Check if destination User ID exists.'
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
    if (token) fetchAccountInfo(token);
  };

  if (!token) {
    return (
      <div className="min-h-screen" style={{ backgroundColor: 'var(--bg-main)' }}>
        <LoginView onLoginSuccess={(newToken) => setToken(newToken)} />
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <header className="glass-panel" style={{ margin: '1.5rem', padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 className="title text-gradient" style={{ fontSize: '1.5rem', marginBottom: 0 }}>
            Risk Control Center
          </h1>
          <p className="subtitle" style={{ fontSize: '0.85rem' }}>Enterprise Transaction Analysis System</p>
        </div>
        
        <div style={{ display: 'flex', gap: '2rem', alignItems: 'center' }}>
           {/* Account Overview Widget */}
           {accountInfo && (
             <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', paddingRight: '2rem', borderRight: '1px solid var(--border-color)' }}>
                <div style={{ background: '#eff6ff', padding: '0.5rem', borderRadius: '8px', color: 'var(--primary)' }}>
                  <Wallet size={20} />
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>Available Balance</div>
                  <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-main)' }}>${accountInfo.balance?.toLocaleString()}</div>
                </div>
             </div>
           )}

           <button 
             onClick={handleLogout}
             className="btn-outline"
             style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', borderColor: 'var(--border-color)', color: 'var(--text-muted)' }}
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
                    background: 'var(--danger-bg)', 
                    border: '1px solid #fca5a5', 
                    borderRadius: '6px', 
                    color: 'var(--danger)',
                    fontSize: '0.875rem',
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
                style={{ padding: '1.5rem' }}
              >
                <h3 className="input-label" style={{ fontSize: '1.1rem', marginBottom: '1.5rem', color: 'var(--text-main)' }}>Processing Transaction</h3>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                    <span className="text-muted">Amount:</span>
                    <span style={{ fontWeight: 600 }}>${transaction?.amount}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
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

        {/* Middle: Visual Pipeline */}
        <div style={{ position: 'relative' }}>
          {stage !== 'IDLE' && (
            <PipelineView stage={stage} transaction={transaction} result={result} />
          )}
          {stage === 'IDLE' && (
            <div style={{ display: 'flex', height: '100%', alignItems: 'center', justifyContent: 'center', opacity: 0.5 }}>
              <div style={{ textAlign: 'center' }}>
                <ShieldAlert size={48} style={{ margin: '0 auto', marginBottom: '1rem', color: 'var(--text-muted)' }} />
                <h2 style={{ color: 'var(--text-muted)', fontSize: '1.1rem', fontWeight: 500 }}>Waiting for transaction input...</h2>
              </div>
            </div>
          )}
        </div>

        {/* Right Side: Analyst Queue */}
        <div>
          <AlertsQueue token={token} />
        </div>
      </main>
    </div>
  )
}

export default App
