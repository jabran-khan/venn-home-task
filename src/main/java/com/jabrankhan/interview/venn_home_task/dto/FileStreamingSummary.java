package com.jabrankhan.interview.venn_home_task.dto;

public record FileStreamingSummary(
        int totalCount,
        int successCount,
        int ignoredCount,
        int rejectCount,
        int errorCount
) {
}
