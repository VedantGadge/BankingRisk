import { useState } from 'react';
import type React from 'react';
import { Send } from 'lucide-react';
import type { TransactionData } from '../App';

interface Props {
  onSubmit: (data: TransactionData) => void;
}

export function TransactionForm({ onSubmit }: Props) {
  const [formData, setFormData] = useState<TransactionData>({
    accountId: 'ACC-88392-XX',
    amount: '450.00',
    transactionType: 'TRANSFER',
    ipAddress: '192.168.1.45',
    location: 'Mumbai, India'
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  return (
    <div className="glass-panel" style={{ padding: '2rem' }}>
      <h2 className="title" style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>
        New Transaction
      </h2>
      
      <form onSubmit={handleSubmit}>
        <div className="input-group">
          <label className="input-label">Account ID</label>
          <input 
            type="text" 
            name="accountId"
            className="input-field" 
            value={formData.accountId}
            onChange={handleChange}
            required
          />
        </div>

        <div className="input-group">
          <label className="input-label">Amount ($)</label>
          <input 
            type="number" 
            name="amount"
            className="input-field" 
            value={formData.amount}
            onChange={handleChange}
            style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--success)' }}
            required
          />
        </div>

        <div className="input-group">
          <label className="input-label">Transaction Type</label>
          <select 
            name="transactionType"
            className="input-field" 
            value={formData.transactionType}
            onChange={handleChange}
          >
            <option value="TRANSFER">Fund Transfer</option>
            <option value="PAYMENT">Bill Payment</option>
            <option value="WITHDRAWAL">ATM Withdrawal</option>
          </select>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div className="input-group">
            <label className="input-label">IP Address</label>
            <input 
              type="text" 
              name="ipAddress"
              className="input-field" 
              value={formData.ipAddress}
              onChange={handleChange}
            />
          </div>
          <div className="input-group">
            <label className="input-label">Location</label>
            <input 
              type="text" 
              name="location"
              className="input-field" 
              value={formData.location}
              onChange={handleChange}
            />
          </div>
        </div>

        <button type="submit" className="btn-primary" style={{ marginTop: '1rem' }}>
          Submit for Analysis <Send size={18} />
        </button>
      </form>
    </div>
  );
}
