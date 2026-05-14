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
            String prompt = buildPrompt(context, ruleResult);
            log.debug("[risk-explanation] llm prompt transactionId={} prompt={}",
                    transactionId, prompt);

            String response = chatClient.prompt()
                    .user(prompt)
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
                You are a fraud analyst assistant.
                
                Explain why this transaction was flagged using the provided risk data only.
                
                Risk score: %d
                Risk level: %s
                
                Triggered rules:
                %s
                
                Risk context:
                %s
                
                Requirements:
                - Keep it concise and professional
                - Mention the top 2 to 4 reasons
                - Do not invent facts not in the input
                - End with a recommendation: approve, review, or block
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