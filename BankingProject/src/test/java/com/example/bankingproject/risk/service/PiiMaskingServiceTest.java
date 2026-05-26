package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskingServiceTest {

    private PiiMaskingService piiMaskingService;

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService();
    }

    @Test
    void anonymizeContext_nullInput_shouldReturnNull() {
        assertNull(piiMaskingService.anonymizeContext(null));
    }

    @Test
    void anonymizeContext_nullIdentitySignals_shouldReturnAnonymizedWithNullIdentity() {
        RiskContext context = RiskContext.builder()
                .transactionId(123L)
                .identitySignals(null)
                .build();

        RiskContext result = piiMaskingService.anonymizeContext(context);
        assertNotNull(result);
        assertEquals(123L, result.getTransactionId());
        assertNull(result.getIdentitySignals());
    }

    @Test
    void anonymizeContext_nullUserIds_shouldReturnNullUserIds() {
        RiskContext context = RiskContext.builder()
                .transactionId(123L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(null)
                        .toUserId(null)
                        .accountAgeDays(5L)
                        .profileRecentlyChanged(false)
                        .build())
                .build();

        RiskContext result = piiMaskingService.anonymizeContext(context);
        assertNotNull(result);
        assertNotNull(result.getIdentitySignals());
        assertNull(result.getIdentitySignals().getFromUserId());
        assertNull(result.getIdentitySignals().getToUserId());
        assertEquals(5L, result.getIdentitySignals().getAccountAgeDays());
    }

    @Test
    void anonymizeContext_shouldReplaceUserIdsWithConsistentTokens() {
        RiskContext context = RiskContext.builder()
                .transactionId(999L)
                .identitySignals(RiskContext.IdentitySignals.builder()
                        .fromUserId(1001L)
                        .toUserId(2002L)
                        .accountAgeDays(15L)
                        .profileRecentlyChanged(true)
                        .build())
                .behaviorSignals(RiskContext.BehaviorSignals.builder()
                        .failedLoginCount(2)
                        .recentTransactionCount(1)
                        .build())
                .moneyFlowSignals(RiskContext.MoneyFlowSignals.builder()
                        .transactionAmount(new BigDecimal("100.00"))
                        .build())
                .build();

        RiskContext anonymized = piiMaskingService.anonymizeContext(context);

        assertNotNull(anonymized);
        assertEquals(999L, anonymized.getTransactionId());

        // Ensure user IDs were replaced/obfuscated (represented as synthetic hash IDs)
        assertNotEquals(1001L, anonymized.getIdentitySignals().getFromUserId());
        assertNotEquals(2002L, anonymized.getIdentitySignals().getToUserId());
        assertEquals(15L, anonymized.getIdentitySignals().getAccountAgeDays());
        assertTrue(anonymized.getIdentitySignals().getProfileRecentlyChanged());
    }

    @Test
    void maskText_edgeCases_shouldReturnAsIs() {
        assertNull(piiMaskingService.maskText(null));
        assertEquals("", piiMaskingService.maskText(""));
        assertEquals("   ", piiMaskingService.maskText("   "));
    }

    @Test
    void maskText_shouldTokenizeEmailsIPsAndPhones() {
        String originalText = "Please contact client John Doe at john.doe@example.com or phone +1-555-555-1234. Login from 192.168.1.10.";

        String masked = piiMaskingService.maskText(originalText);

        assertNotNull(masked);
        assertFalse(masked.contains("john.doe@example.com"));
        assertFalse(masked.contains("+1-555-555-1234"));
        assertFalse(masked.contains("192.168.1.10"));

        assertTrue(masked.contains("[EMAIL_TOKEN_"));
        assertTrue(masked.contains("[PHONE_TOKEN_"));
        assertTrue(masked.contains("[IP_TOKEN_"));
    }

    @Test
    void maskText_repeatedValues_shouldReuseTokens() {
        String text = "Send to support@bank.com. Again, the email is support@bank.com.";
        String masked = piiMaskingService.maskText(text);

        // Extract the token dynamically
        int firstStart = masked.indexOf("EMAIL_TOKEN_");
        int firstEnd = masked.indexOf("]", firstStart);
        String firstToken = masked.substring(firstStart, firstEnd);

        int lastStart = masked.lastIndexOf("EMAIL_TOKEN_");
        int lastEnd = masked.indexOf("]", lastStart);
        String lastToken = masked.substring(lastStart, lastEnd);

        assertEquals(firstToken, lastToken);
    }

    @Test
    void rehydrateText_edgeCases_shouldReturnAsIs() {
        assertNull(piiMaskingService.rehydrateText(null));
        assertEquals("", piiMaskingService.rehydrateText(""));
        assertEquals("   ", piiMaskingService.rehydrateText("   "));
    }

    @Test
    void rehydrateText_shouldRestoreOriginalValuesFromTokens() {
        String originalText = "Suspicious transaction from IP 192.168.1.50 initiated by customer support email test@bank.com.";

        // Mask it first
        String masked = piiMaskingService.maskText(originalText);
        assertTrue(masked.contains("[IP_TOKEN_"));
        assertTrue(masked.contains("[EMAIL_TOKEN_"));

        // Rehydrate it
        String rehydrated = piiMaskingService.rehydrateText(masked);

        // Ensure exact restoration of the values
        assertEquals(originalText, rehydrated);
    }

    @Test
    void rehydrateText_bareTokenWithoutBrackets_shouldStillRehydrate() {
        String rawValue = "confidential@secret.com";
        String originalText = "Email " + rawValue;

        // Mask first to create the mapping
        String masked = piiMaskingService.maskText(originalText);

        // Extract bare token dynamically from [EMAIL_TOKEN_XXXX]
        int start = masked.indexOf("EMAIL_TOKEN_");
        int end = masked.indexOf("]", start);
        String token = masked.substring(start, end);

        // Test rehydrating a string containing just the bare token
        String bareTest = "Please reply to " + token;
        String rehydrated = piiMaskingService.rehydrateText(bareTest);

        assertEquals("Please reply to confidential@secret.com", rehydrated);
    }

    @Test
    void clearRegistry_shouldRemoveMappings() {
        String originalText = "Refund required for user1@bank.com";
        String masked = piiMaskingService.maskText(originalText);

        piiMaskingService.clearRegistry();

        String rehydrated = piiMaskingService.rehydrateText(masked);
        // Since registry was cleared, it can no longer rehydrate and should return the masked text unchanged
        assertEquals(masked, rehydrated);
    }

    @Test
    void tokenMapping_lombokVerification() {
        PiiMaskingService.TokenMapping mapping1 = new PiiMaskingService.TokenMapping("val", "tok", "type");
        PiiMaskingService.TokenMapping mapping2 = new PiiMaskingService.TokenMapping("val", "tok", "type");

        assertEquals(mapping1, mapping2);
        assertEquals(mapping1.hashCode(), mapping2.hashCode());
        assertEquals("val", mapping1.getOriginalValue());
        assertEquals("tok", mapping1.getToken());
        assertEquals("type", mapping1.getPiiType());
        assertNotNull(mapping1.toString());

        mapping1.setOriginalValue("new_val");
        assertEquals("new_val", mapping1.getOriginalValue());
    }
}
