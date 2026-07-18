package com.jabrankhan.interview.venn_home_task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LoadRequest(
        String id,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("load_amount") String loadAmountStr,
        ZonedDateTime time
) {
    public BigDecimal getCleanAmount() {
        if (loadAmountStr == null) return BigDecimal.ZERO;
        return new BigDecimal(loadAmountStr.replace("$", "").trim());
    }
}
