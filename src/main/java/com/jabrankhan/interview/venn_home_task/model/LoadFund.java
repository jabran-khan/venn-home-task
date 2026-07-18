package com.jabrankhan.interview.venn_home_task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "load_fund")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadFund {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "load_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal loadAmount;

    @Column(name = "time", nullable = false)
    private ZonedDateTime time;

    // we want to store every input in the database
    //   therefore this is required to see if the transaction was accepted or not
    @Column(nullable = false)
    private boolean accepted;
}
