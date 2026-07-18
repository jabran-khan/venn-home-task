package com.jabrankhan.interview.venn_home_task.repository;

import com.jabrankhan.interview.venn_home_task.model.CustomerLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CustomerLimitRepository extends JpaRepository<CustomerLimit, UUID> {
    Optional<CustomerLimit> findByCustomerIdAndCurrentDay(String customerId, LocalDate currentDay);
}
