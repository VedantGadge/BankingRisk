# RiskContext Signals — Detailed Architecture & Banking Intelligence Guide

## Overview

`RiskContext` is the core AI evidence object of the banking risk intelligence platform.

It is NOT:

* a database entity,
* a request DTO,
* or a response DTO.

Instead, it is a:

```text
Structured AI Evidence Packet
```

used by the Risk Engine and Spring AI to perform:

* fraud analysis,
* AML analysis,
* behavioral anomaly detection,
* alert triage,
* analyst assistance.

---

# Core Philosophy

The AI should NOT guess risk.

Instead:

```text
Banking data
→ Signal extraction
→ Structured evidence
→ AI reasoning
→ Risk alert
```

The system first computes meaningful risk signals from operational banking data and then allows the AI model to reason over those signals.

This is called:

```text
Structured Retrieval + Context Engineering
```

---

# High-Level Flow

```text
Transaction occurs
→ RiskContextBuilder executes
→ Fetches data from DB/services
→ Computes behavioral + AML signals
→ Builds RiskContext
→ Sends context to Spring AI
→ AI generates risk triage output
```

---

# RiskContext Structure

```text
RiskContext
├── IdentitySignals
├── BehaviorSignals
├── DeviceSignals
├── BeneficiarySignals
├── HistoricalSignals
├── AmlSignals
└── MoneyFlowSignals
```

---

# 1. IdentitySignals

Identity signals measure:

* trustworthiness of account identity,
* onboarding quality,
* identity stability.

These are foundational banking risk indicators.

---

## `fromUserId`

### Represents

The sender account initiating the transfer.

### Signal Type

Identity anchor signal.

### Why Important

Risk analysis is usually centered around:

* sender behavior,
* sender history,
* sender anomalies.

---

## `toUserId`

### Represents

The recipient account.

### Signal Type

Recipient identity signal.

### Why Important

Many fraud and AML cases are recipient-driven:

* mule accounts,
* laundering accounts,
* scam beneficiaries.

---

## `accountAgeDays`

### Represents

Age of the sender account in days.

### Signal Type

Account maturity signal.

### Why Important

New accounts are statistically higher risk because:

* fraudsters create fresh accounts,
* mule accounts are short-lived,
* synthetic identities are often newly created.

### Example

```text
2-day-old account
+ large transfer
= suspicious
```

---

## `kycComplete`

### Represents

Whether KYC verification is fully completed.

### Signal Type

Compliance trust signal.

### Why Important

Incomplete KYC increases:

* fraud risk,
* AML risk,
* regulatory exposure.

---

## `kycMismatch`

### Represents

Whether submitted identity details appear inconsistent.

### Signal Type

Identity inconsistency signal.

### Why Important

Mismatch between:

* PAN,
* Aadhaar,
* address,
* device region,
* account behavior

can indicate:

* synthetic identities,
* account takeover,
* mule networks.

---

## `profileRecentlyChanged`

### Represents

Recent changes to:

* password,
* email,
* phone,
* address.

### Signal Type

Identity change signal.

### Why Important

Fraudsters often:

* change account credentials,
* modify recovery information,
  before extracting money.

---

## `firstTransactionAfterSignup`

### Represents

Whether this is the first transfer after account creation.

### Signal Type

Onboarding anomaly signal.

### Why Important

Fraud accounts often:

* onboard quickly,
* immediately begin transferring money.

---

# 2. BehaviorSignals

Behavior signals detect:

* abnormal activity,
* unusual user patterns,
* behavioral drift.

Behavioral anomalies are among the strongest fraud indicators.

---

## `recentTransactionCount`

### Represents

Number of recent transactions in a defined time window.

### Signal Type

Velocity signal.

### Why Important

Fraud often involves:

* bursts of activity,
* automated movement,
* rapid money extraction.

---

## `totalRecentTransactionAmount`

### Represents

Total amount moved recently.

### Signal Type

Financial intensity signal.

### Why Important

Sudden liquidity movement can indicate:

* laundering,
* cash-out attacks,
* mule activity.

---

## `averageRecentTransactionAmount`

### Represents

Average historical transaction size.

### Signal Type

Behavioral baseline signal.

### Why Important

Risk systems compare:

```text
Current amount
VS
Historical norm
```

### Example

```text
Historical avg = ₹2,000
Current transfer = ₹2,00,000
```

Large deviation indicates anomaly.

---

## `transactionVelocitySpike`

### Represents

Whether recent activity exceeds normal behavior.

### Signal Type

Behavioral spike signal.

### Why Important

Fraud systems monitor:

* transaction frequency acceleration,
* sudden account activity spikes.

---

## `amountSpike`

### Represents

Whether transaction amount is unusually high relative to history.

### Signal Type

Financial anomaly signal.

### Why Important

Amount anomalies are key indicators of:

* account takeover,
* laundering,
* scam transfers.

---

## `unusualTimeOfDay`

### Represents

Whether transaction timing is abnormal.

### Signal Type

Temporal anomaly signal.

### Why Important

Fraud often occurs:

* late night,
* outside normal user hours,
* during low-monitoring periods.

---

## `weekendOrHolidayActivity`

### Represents

Unusual activity during weekends/holidays.

### Signal Type

Temporal risk signal.

### Why Important

Certain fraud operations intensify during:

* weekends,
* holidays,
* banking downtimes.

---

## `failedLoginsBeforeTransfer`

### Represents

Failed login attempts before transfer.

### Signal Type

Authentication anomaly signal.

### Why Important

Classic account takeover pattern:

```text
Multiple failed logins
→ successful login
→ large transfer
```

---

## `failedLoginCount`

### Represents

Number of recent failed login attempts.

### Signal Type

Authentication risk signal.

### Why Important

Brute-force attempts often precede:

* credential compromise,
* account takeover.

---

# 3. DeviceSignals

Device signals evaluate:

* session trust,
* device familiarity,
* location consistency.

These are heavily used in modern fraud systems.

---

## `newDevice`

### Represents

Whether login came from unseen device.

### Signal Type

Device trust signal.

### Why Important

New devices increase:

* takeover risk,
* credential theft suspicion.

---

## `newBrowser`

### Represents

Whether browser fingerprint changed.

### Signal Type

Session anomaly signal.

### Why Important

Unexpected browser changes may indicate:

* session hijacking,
* bot activity.

---

## `ipChanged`

### Represents

IP address change.

### Signal Type

Network anomaly signal.

### Why Important

Sudden IP shifts may indicate:

* VPN abuse,
* proxy usage,
* attacker activity.

---

## `locationChanged`

### Represents

Geographic location change.

### Signal Type

Geo anomaly signal.

### Why Important

Behavior inconsistent with user geography is suspicious.

---

## `impossibleTravel`

### Represents

Physically impossible travel between sessions.

### Signal Type

Geo-temporal anomaly signal.

### Example

```text
Mumbai login
→ 10 minutes later
→ London login
```

---

## `suspiciousSession`

### Represents

Whether session appears unsafe.

### Signal Type

Session integrity signal.

### Why Important

Can indicate:

* hijacked sessions,
* automation,
* scripted abuse.

---

## `sessionAgeMinutes`

### Represents

Session age before transaction.

### Signal Type

Session maturity signal.

### Why Important

Fresh sessions performing large transfers may be suspicious.

---

## `deviceRiskScore`

### Represents

Internal device trust score.

### Signal Type

Device reputation signal.

### Why Important

Known risky devices should increase scrutiny.

---

# 4. BeneficiarySignals

Beneficiary signals analyze:

* recipient trust,
* recipient network behavior,
* recipient relationships.

---

## `beneficiaryAgeDays`

### Represents

Recipient account age.

### Signal Type

Recipient maturity signal.

### Why Important

Fresh recipients are higher risk.

---

## `newBeneficiary`

### Represents

Whether recipient is new to sender.

### Signal Type

Relationship novelty signal.

### Why Important

Fraud frequently targets:

* first-time transfers,
* unfamiliar recipients.

---

## `firstTimeTransferToRecipient`

### Represents

Whether sender has transferred before.

### Signal Type

Relationship history signal.

### Why Important

No historical trust exists.

---

## `beneficiarySeenInPreviousAlerts`

### Represents

Recipient appeared in prior alerts.

### Signal Type

Historical recipient risk signal.

### Why Important

Repeated suspicious recipients may indicate mule networks.

---

## `beneficiaryRiskScore`

### Represents

Internal recipient risk rating.

### Signal Type

Recipient reputation signal.

---

## `numberOfSendersToSameBeneficiary`

### Represents

How many accounts sent money to recipient.

### Signal Type

Fan-in AML signal.

### Why Important

High fan-in may indicate:

* laundering hub,
* mule account.

---

## `nameMismatchSuspected`

### Represents

Potential mismatch in beneficiary naming.

### Signal Type

Identity inconsistency signal.

---

# 5. HistoricalSignals

Historical signals provide long-term behavioral memory.

These help identify repeated or evolving suspicious patterns.

---

## `previousAlertsCount`

### Represents

Number of previous risk alerts linked to account.

### Signal Type

Historical reputation signal.

### Why Important

Repeated suspicious behavior increases confidence in future suspicion.

---

## `previousRejectedTransfersCount`

### Represents

How many transfers were previously rejected.

### Signal Type

Historical fraud friction signal.

### Why Important

Repeated rejected activity may indicate:

* fraud attempts,
* laundering attempts,
* suspicious transfer experimentation.

---

## `repeatedDisputesCount`

### Represents

Repeated disputes associated with account.

### Signal Type

Dispute anomaly signal.

### Why Important

Excessive disputes can indicate:

* chargeback abuse,
* synthetic fraud,
* mule activity.

---

## `linkedAccountsCount`

### Represents

Number of connected accounts.

### Signal Type

Network relationship signal.

### Why Important

Fraud rings often operate across multiple linked accounts.

---

## `sharedDeviceAccountsCount`

### Represents

How many accounts use same device.

### Signal Type

Shared infrastructure signal.

### Why Important

Fraud networks frequently:

* share devices,
* reuse browsers,
* reuse infrastructure.

---

## `sharedRecipientAccountsCount`

### Represents

How many accounts transfer to same recipient.

### Signal Type

Recipient concentration signal.

### Why Important

Shared recipients may indicate:

* mule hubs,
* laundering nodes,
* coordinated fraud.

---

## `previousAlertsSummary`

### Represents

Natural language summary of previous alerts.

### Signal Type

Historical reasoning context.

### Why Important

Helps AI reason over:

* repeated patterns,
* escalation,
* historical similarity.

---

# 6. AmlSignals

AML signals focus on:

* money laundering,
* layering,
* structuring,
* mule networks,
* hidden money flow.

These are among the most important enterprise banking signals.

---

## `highAmountToMultipleSmallAccounts`

### Represents

Large amount distributed to many low-balance accounts.

### Signal Type

Fan-out AML signal.

### Why Important

Classic laundering/layering pattern:

```text
One source
→ many small accounts
→ redistributed again
```

---

## `structuringSuspected`

### Represents

Possible splitting of money into smaller transfers.

### Signal Type

Threshold evasion signal.

### Why Important

Fraudsters split transactions to avoid AML detection thresholds.

---

## `smurfingSuspected`

### Represents

Possible coordinated movement using many accounts.

### Signal Type

Distributed laundering signal.

### Why Important

Smurfing uses multiple accounts to hide origin of funds.

---

## `layeringSuspected`

### Represents

Possible multi-hop movement of money.

### Signal Type

AML layering signal.

### Why Important

Layering obscures money origin through complex transfers.

---

## `rapidCashOutSuspected`

### Represents

Funds quickly withdrawn or moved after arrival.

### Signal Type

Cash-out signal.

### Why Important

Rapid extraction is common in:

* account takeover,
* laundering,
* mule operations.

---

## `circularTransferSuspected`

### Represents

Money appears to move in loops.

### Signal Type

Circular flow signal.

### Why Important

Circular movement may indicate:

* laundering,
* synthetic activity,
* hidden ownership.

---

## `muleAccountPatternSuspected`

### Represents

Recipient behaves like pass-through account.

### Signal Type

Mule behavior signal.

### Why Important

Mule accounts:

* receive money,
* rapidly redistribute,
* retain little balance.

---

## `fanOutCount`

### Represents

Number of outbound recipients.

### Signal Type

Fan-out network signal.

### Why Important

High fan-out increases laundering suspicion.

---

## `fanInCount`

### Represents

Number of inbound senders.

### Signal Type

Fan-in network signal.

### Why Important

High fan-in may indicate collection/mule accounts.

---

## `totalOutboundAmount24h`

### Represents

Total outbound movement in last 24h.

### Signal Type

Liquidity movement signal.

---

## `totalInboundAmount24h`

### Represents

Total inbound movement in last 24h.

### Signal Type

Liquidity intake signal.

---

## `maxRecipientBalance`

### Represents

Largest recipient balance.

### Signal Type

Recipient financial profile signal.

### Why Important

Sending large amounts to low-balance accounts is suspicious.

---

## `numberOfSmallBalanceRecipients`

### Represents

How many recipients are low-balance accounts.

### Signal Type

AML recipient-distribution signal.

### Why Important

Laundering often distributes funds across weak/fresh accounts.

---

## `amlPatternSummary`

### Represents

Natural language AML summary.

### Signal Type

Human-readable AML reasoning context.

---

# 7. MoneyFlowSignals

Money flow signals analyze:

* movement intensity,
* transfer behavior,
* financial deviations.

---

## `transactionAmount`

### Represents

Current transaction amount.

### Signal Type

Core financial signal.

---

## `currentBalance`

### Represents

Current account balance.

### Signal Type

Liquidity context signal.

---

## `amountToBalanceRatio`

### Represents

Transfer amount relative to account balance.

### Signal Type

Financial stress signal.

### Example

```text
₹50k transfer
from ₹55k balance
```

Much more suspicious than:

```text
₹50k transfer
from ₹50 lakh balance
```

---

## `amountVsHistoricalAverageRatio`

### Represents

Current transfer relative to historical average.

### Signal Type

Behavior deviation signal.

---

## `salaryCycleDeviation`

### Represents

Deviation from expected salary behavior.

### Signal Type

Income-flow anomaly signal.

### Why Important

Users usually exhibit salary-linked financial patterns.

---

## `transfersLastHour`

### Represents

Transfers in last hour.

### Signal Type

Short-term velocity signal.

---

## `transfersLast24h`

### Represents

Transfers in last 24 hours.

### Signal Type

Daily activity signal.

---

## `uniqueRecipientsLast24h`

### Represents

Distinct recipients in last 24h.

### Signal Type

Distribution signal.

### Why Important

High recipient spread may indicate laundering.

---

## `cashOutPattern`

### Represents

Rapid outflow after incoming funds.

### Signal Type

Cash extraction signal.

### Why Important

Classic laundering/account takeover behavior.

---

# Final Architectural Insight

The real intelligence of the system is NOT:

```text
just calling an LLM
```

The real intelligence comes from:

```text
meaningful signal engineering
+ structured retrieval
+ contextual reasoning
```

---

# Final Mental Model

```text
Raw Banking Data
→ Signal Engineering
→ Structured Evidence
→ AI Risk Reasoning
→ Analyst Assistance
→ Risk Decision Support
```

This is how modern AI-assisted banking risk systems are conceptually designed.
