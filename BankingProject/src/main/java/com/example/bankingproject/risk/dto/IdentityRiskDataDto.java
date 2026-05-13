package com.example.bankingproject.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityRiskDataDto {
    private Long fromUserId;
    private Long toUserId;
    private Long accountAgeDays;
    private Boolean profileRecentlyChanged;
}
