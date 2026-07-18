package com.jabrankhan.interview.venn_home_task.dto;

public record LoadResponse(
        String id,
        String customer_id,
        boolean accepted
) {
}
