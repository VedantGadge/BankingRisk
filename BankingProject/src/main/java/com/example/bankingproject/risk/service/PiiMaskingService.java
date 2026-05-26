package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise PII Masking and Local Tokenization (The Anonymizer Pattern) Service.
 *
 * This service ensures that Personally Identifiable Information (PII) such as User IDs,
 * IP addresses, emails, and account details are fully tokenized before being sent to
 * third-party LLMs (public cloud). It also provides a local re-hydration mechanism so
 * compliance analysts see the true, non-anonymized data inside the secure corporate firewall.
 */
@Service
@Slf4j
public class PiiMaskingService {

    // Simple regex patterns for common PII formats
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );
    private static final Pattern IPv4_PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b\\+?[0-9]{1,3}?[-.\\s]?(?:\\(?[0-9]{3}\\)?|[0-9]{3})[-.\\s]?[0-9]{3}[-.\\s]?[0-9]{4}\\b"
    );

    /**
     * Local token registry stored entirely in memory (within the bank's secure perimeter).
     * In a production environment, this should be backed by a short-lived Redis cache (TTL e.g., 24 hours).
     */
    private final Map<String, TokenMapping> tokenRegistry = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    public static class TokenMapping {
        private String originalValue;
        private String token;
        private String piiType;
    }

    /**
     * Anonymizes a RiskContext by replacing direct user IDs with contextual tokens (e.g., SENDER_USER_1).
     * This returns a copy of the context with anonymized database primary keys.
     */
    public RiskContext anonymizeContext(RiskContext context) {
        if (context == null) return null;

        log.debug("[pii-masking] anonymizing RiskContext for transactionId={}", context.getTransactionId());

        RiskContext.IdentitySignals identity = context.getIdentitySignals();
        RiskContext.IdentitySignals anonymizedIdentity = null;

        if (identity != null) {
            String fromUserToken = identity.getFromUserId() != null ? getOrCreateToken(String.valueOf(identity.getFromUserId()), "USER_ID", "SENDER_USER") : null;
            String toUserToken = identity.getToUserId() != null ? getOrCreateToken(String.valueOf(identity.getToUserId()), "USER_ID", "RECEIVER_USER") : null;

            anonymizedIdentity = RiskContext.IdentitySignals.builder()
                    .fromUserId(fromUserToken != null ? Math.abs((long) fromUserToken.hashCode()) : null)
                    .toUserId(toUserToken != null ? Math.abs((long) toUserToken.hashCode()) : null)
                    .accountAgeDays(identity.getAccountAgeDays())
                    .profileRecentlyChanged(identity.getProfileRecentlyChanged())
                    .build();
        }

        return RiskContext.builder()
                .transactionId(context.getTransactionId())
                .identitySignals(anonymizedIdentity)
                .behaviorSignals(context.getBehaviorSignals())
                .moneyFlowSignals(context.getMoneyFlowSignals())
                .build();
    }

    /**
     * Scans and sanitizes any raw unstructured text (e.g. system messages or notes)
     * using regex pattern matching, replacing emails, IPs, and phones with unique tokens.
     */
    public String maskText(String rawText) {
        if (rawText == null || rawText.isBlank()) return rawText;

        String masked = rawText;

        // Mask Emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(masked);
        while (emailMatcher.find()) {
            String email = emailMatcher.group();
            String token = getOrCreateToken(email, "EMAIL", "EMAIL_TOKEN");
            masked = masked.replace(email, "[" + token + "]");
        }

        // Mask IPs
        Matcher ipMatcher = IPv4_PATTERN.matcher(masked);
        while (ipMatcher.find()) {
            String ip = ipMatcher.group();
            String token = getOrCreateToken(ip, "IP_ADDRESS", "IP_TOKEN");
            masked = masked.replace(ip, "[" + token + "]");
        }

        // Mask Phone Numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(masked);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            String token = getOrCreateToken(phone, "PHONE_NUMBER", "PHONE_TOKEN");
            masked = masked.replace(phone, "[" + token + "]");
        }

        return masked;
    }

    /**
     * Rehydrates an anonymized text string back to its original values using the local token registry.
     * This is called securely inside the bank's internal controller before serving data to an authenticated analyst's browser.
     */
    public String rehydrateText(String maskedText) {
        if (maskedText == null || maskedText.isBlank()) return maskedText;

        String rehydrated = maskedText;
        for (TokenMapping mapping : tokenRegistry.values()) {
            String tokenContainer = "[" + mapping.getToken() + "]";
            if (rehydrated.contains(tokenContainer)) {
                rehydrated = rehydrated.replace(tokenContainer, mapping.getOriginalValue());
            }
            // Also replace bare tokens just in case the brackets were stripped by the LLM
            if (rehydrated.contains(mapping.getToken())) {
                rehydrated = rehydrated.replace(mapping.getToken(), mapping.getOriginalValue());
            }
        }
        return rehydrated;
    }

    /**
     * Helper to lookup or dynamically register a token for a given value to ensure consistency
     * (e.g. the same User ID always maps to the same token within this session's context).
     */
    private String getOrCreateToken(String originalValue, String piiType, String prefix) {
        if (originalValue == null) return null;

        // Check if value already has a registered token to keep consistent references
        return tokenRegistry.values().stream()
                .filter(m -> m.getOriginalValue().equals(originalValue))
                .map(TokenMapping::getToken)
                .findFirst()
                .orElseGet(() -> {
                    String uuidTail = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    String token = prefix + "_" + uuidTail;
                    TokenMapping mapping = new TokenMapping(originalValue, token, piiType);
                    tokenRegistry.put(token, mapping);
                    log.debug("[pii-masking] Registered token: {} -> {}", token, piiType);
                    return token;
                });
    }

    /**
     * Clear the registry mapping (typically run after the batch or transaction context concludes).
     */
    public void clearRegistry() {
        tokenRegistry.clear();
        log.debug("[pii-masking] Token registry cleared successfully.");
    }
}
