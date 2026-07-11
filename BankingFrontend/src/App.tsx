import { useState } from 'react'
import { Activity, ShieldAlert } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { TransactionForm } from './components/TransactionForm'
import { PipelineView } from './components/PipelineView'
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
  const [stage, setStage] = useState<SimulationStage>('IDLE');
  const [transaction, setTransaction] = useState<TransactionData | null>(null);
  const [result, setResult] = useState<any>(null);

  const handleSimulate = async (data: TransactionData) => {
    setTransaction(data);
    setResult(null);
    
    // Step 1: Start Masking
    setStage('MASKING');
    await new Promise(r => setTimeout(r, 1500));
    
    // Step 2: Rules Engine
    setStage('RULES_ENGINE');
    await new Promise(r => setTimeout(r, 2000));
    
    // Step 3: LLM Analysis
    setStage('LLM_ANALYSIS');
    
    // In a real scenario, we would make a POST to the backend here.
    // We will simulate the backend processing delay for visual effect.
    await new Promise(r => setTimeout(r, 3500));
    
    // Mock Result
    const mockResult = {
      score: parseInt(data.amount) > 10000 ? 85 : 20,
      decision: parseInt(data.amount) > 10000 ? 'FLAGGED' : 'APPROVED',
      justification: parseInt(data.amount) > 10000 
        ? "The transaction amount exceeds typical baseline behavior for this account. Additionally, the IP address originates from a high-risk location. Recommend manual review."
        : "Transaction is within normal parameters. Velocity rules passed. IP location matches known history."
    };
    
    setResult(mockResult);
    setStage('COMPLETED');
  };

  const resetSimulation = () => {
    setStage('IDLE');
    setTransaction(null);
    setResult(null);
  };

  return (
    <div className="min-h-screen">
      <header className="glass-panel" style={{ margin: '2rem', padding: '1.5rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 className="title text-gradient" style={{ fontSize: '1.75rem', marginBottom: 0 }}>
            Risk Evaluation Engine
          </h1>
          <p className="subtitle" style={{ fontSize: '0.9rem' }}>Real-time transaction analysis simulation</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
           <div className={`node-icon-wrapper ${stage !== 'IDLE' ? 'active' : ''}`} style={{ width: '40px', height: '40px' }}>
              <Activity size={20} />
           </div>
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
                <TransactionForm onSubmit={handleSimulate} />
              </motion.div>
            ) : (
              <motion.div
                key="summary"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="glass-panel p-6"
                style={{ padding: '2rem' }}
              >
                <h3 className="input-label" style={{ fontSize: '1.1rem', marginBottom: '1.5rem', color: 'var(--text-main)' }}>Processing Transaction</h3>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Amount:</span>
                    <span style={{ fontWeight: 600 }}>${transaction?.amount}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="text-muted">Account:</span>
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
