package com.jabrankhan.interview.venn_home_task.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "customer_limits",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "current_day"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// stores the customer limits are time windows which we can calculate and grab as needed
public class CustomerLimit {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "current_day", nullable = false)
    private LocalDate currentDay;

    @Column(name = "daily_total_amount", nullable = false)
    private BigDecimal dailyTotalAmount = BigDecimal.ZERO;

    @Column(name = "weekly_total_amount", nullable = false)
    private BigDecimal weeklyTotalAmount = BigDecimal.ZERO;
}
