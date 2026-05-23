package com.example.bankingproject.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveAlertRequest {

    @NotBlank(message = "Action is required.")
    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Action must be either APPROVE or REJECT")
    private String action;

    @NotBlank(message = "Notes are required for triage audit.")
    private String notes;
}
