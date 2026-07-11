import { useState } from 'react';
import type React from 'react';
import { LogIn } from 'lucide-react';
import axios from 'axios';

interface Props {
  onLoginSuccess: (token: string) => void;
}

export function LoginView({ onLoginSuccess }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Connect to the OCI Spring Boot backend
      const response = await axios.post('http://144.24.122.133:8080/api/auth/login', {
        email,
        password
      });

      if (response.data && response.data.token) {
        onLoginSuccess(response.data.token);
      } else {
        setError('Login failed: Token not returned.');
      }
    } catch (err: any) {
      console.error(err);
      setError(
        err.response?.data?.message || 
        'Could not connect to backend. Ensure VM is running and CORS is enabled.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
      <div className="glass-panel" style={{ padding: '2.5rem', width: '100%', maxWidth: '400px' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h2 className="title text-gradient" style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>
            Risk Portal
          </h2>
          <p className="subtitle" style={{ fontSize: '0.95rem' }}>Authenticate to access the risk simulation</p>
        </div>

        {error && (
          <div style={{ 
            padding: '0.75rem 1rem', 
            background: 'rgba(239, 68, 68, 0.1)', 
            border: '1px solid rgba(239, 68, 68, 0.3)', 
            borderRadius: '8px', 
            color: 'var(--danger)',
            fontSize: '0.9rem',
            marginBottom: '1.5rem'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label className="input-label">Email Address</label>
            <input
              type="email"
              className="input-field"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. user@example.com"
              required
            />
          </div>

          <div className="input-group" style={{ marginBottom: '2rem' }}>
            <label className="input-label">Password</label>
            <input
              type="password"
              className="input-field"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Authenticating...' : 'Sign In'} <LogIn size={18} />
          </button>
        </form>
      </div>
    </div>
  );
}
