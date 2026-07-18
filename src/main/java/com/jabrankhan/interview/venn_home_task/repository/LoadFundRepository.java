package com.jabrankhan.interview.venn_home_task.repository;

import com.jabrankhan.interview.venn_home_task.model.LoadFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface LoadFundRepository extends JpaRepository<LoadFund, String> {

    @Query(
            """
            SELECT COALESCE(SUM(f.loadAmount), 0) FROM LoadFund f
            WHERE f.customerId = :customerId
            AND f.accepted = true
            AND f.time >= :weekStart
            AND f.time < :fundTime
            """
    )
    BigDecimal sumAcceptedAmountsForCurrentWeek(
            @Param("customerId") String customerId,
            @Param("weekStart")ZonedDateTime weekStart,
            @Param("fundTime") ZonedDateTime fundTime
    );

    Optional<LoadFund> findByIdAndCustomerId(String id, String customerId);
}
