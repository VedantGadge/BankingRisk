CREATE TABLE IF NOT EXISTS risk_alerts (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    risk_score INTEGER NOT NULL,
    summary VARCHAR(2000),
    reason_codes VARCHAR(4000),
    risk_status VARCHAR(50) NOT NULL,
    recommended_action VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
