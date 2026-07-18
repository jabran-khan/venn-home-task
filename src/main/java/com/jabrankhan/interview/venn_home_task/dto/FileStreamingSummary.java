package com.jabrankhan.interview.venn_home_task.dto;

import java.util.List;

public record FileStreamingSummary(
        int totalCount,
        int successCount,
        int rejectCount,
        int errorCount
) {
}
