package com.example.bankingproject.risk.service;

import com.example.bankingproject.risk.dto.RiskContext;
import com.example.bankingproject.risk.dto.RiskRuleResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskExplanationService {

    private final ChatClient chatClient;

    public String explain(RiskContext context, RiskRuleResultDto ruleResult) {
        Long transactionId = context != null ? context.getTransactionId() : null;
        log.info("[risk-explanation] llm call start transactionId={} riskLevel={} riskScore={}",
                transactionId,
                ruleResult != null ? ruleResult.getRiskLevel() : null,
                ruleResult != null ? ruleResult.getRiskScore() : null);

        try {
            String systemText = "You are a senior banking fraud analyst assistant. Your absolute most critical rule is to ONLY use the provided risk data to explain the transaction. You must NEVER invent, hallucinate, or mention rules, signals, or facts that are not explicitly present in the user's input. Keep the explanation concise and professional, mention the top 2 to 4 reasons, and end with a recommendation (approve, review, or block).";
            String userText = buildPrompt(context, ruleResult);
            log.debug("[risk-explanation] llm prompt transactionId={} systemPrompt={} userPrompt={}",
                    transactionId, systemText, userText);

            String response = chatClient.prompt()
                    .system(systemText)
                    .user(userText)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("[risk-explanation] llm call returned empty response transactionId={}", transactionId);
                String fallback = fallbackExplanation(ruleResult);
                log.info("[risk-explanation] fallback response transactionId={} response={}",
                        transactionId, fallback);
                return fallback;
            }

            log.info("[risk-explanation] llm response transactionId={} response={}",
                    transactionId, response);
            log.info("[risk-explanation] llm call success transactionId={}", transactionId);
            return response;

        } catch (Exception e) {
            log.warn("[risk-explanation] Spring AI explanation failed, using fallback explanation transactionId={}",
                    transactionId, e);
            String fallback = fallbackExplanation(ruleResult);
            log.info("[risk-explanation] fallback response transactionId={} response={}",
                    transactionId, fallback);
            log.warn("[risk-explanation] llm call failed transactionId={} reason={}",
                    transactionId, e.getMessage());
            return fallback;
        }
    }

    private String buildPrompt(RiskContext context, RiskRuleResultDto ruleResult) {
        return """
                Risk score: %d
                Risk level: %s
                
                Triggered rules:
                %s
                
                Risk context:
                %s
                """.formatted(
                ruleResult.getRiskScore(),
                ruleResult.getRiskLevel(),
                String.join("\n", ruleResult.getTriggeredRules()),
                context
        );
    }

    private String fallbackExplanation(RiskRuleResultDto ruleResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk level: ")
                .append(ruleResult.getRiskLevel())
                .append(" (score=")
                .append(ruleResult.getRiskScore())
                .append("). ");

        if (ruleResult.getTriggeredRules() != null && !ruleResult.getTriggeredRules().isEmpty()) {
            sb.append("Triggered reasons: ")
                    .append(String.join("; ", ruleResult.getTriggeredRules()))
                    .append(". ");
        }

        if ("HIGH".equalsIgnoreCase(ruleResult.getRiskLevel())) {
            sb.append("Recommended action: block or manual review.");
        } else if ("MEDIUM".equalsIgnoreCase(ruleResult.getRiskLevel())) {
            sb.append("Recommended action: review before approval.");
        } else {
            sb.append("Recommended action: approve with monitoring.");
        }

        return sb.toString();
    }
}