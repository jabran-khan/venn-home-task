package com.jabrankhan.interview.venn_home_task.service;

import com.jabrankhan.interview.venn_home_task.dto.LoadRequest;
import com.jabrankhan.interview.venn_home_task.dto.LoadResponse;
import com.jabrankhan.interview.venn_home_task.model.CustomerLimit;
import com.jabrankhan.interview.venn_home_task.model.LoadFund;
import com.jabrankhan.interview.venn_home_task.repository.CustomerLimitRepository;
import com.jabrankhan.interview.venn_home_task.repository.LoadFundRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Slf4j
@Service
public class LoadFundService {

    private final LoadFundRepository loadFundRepository;
    private final CustomerLimitRepository limitRepository;

    @Value("${app.limits.max-funds-per-day:3}")
    private int maxFundsPerDay;

    @Value("${app.limits.daily-limit:5000.00}")
    private BigDecimal dailyMaxLimit;

    @Value("${app.limits.weekly-max:20000.00}")
    private BigDecimal weeklyMaxLimit;

    public LoadFundService(LoadFundRepository loadFundRepository, CustomerLimitRepository limitRepository) {
        this.loadFundRepository = loadFundRepository;
        this.limitRepository = limitRepository;
    }

    @Transactional
    public Optional<LoadResponse> loadFunds(LoadRequest request) {
        log.info("loading funds information: [id: {}, customerId: {}, amount: {}, time: {}]",
                request.id(),
                request.customerId(),
                request.loadAmountStr(),
                request.time()
        );
        var existingFund = loadFundRepository.findByIdAndCustomerId(request.id(), request.customerId());
        if (existingFund.isPresent()) {
            log.warn("duplicate fund id: {}", request.id());
            return Optional.empty();
        }

        BigDecimal amount = request.getCleanAmount();
        LocalDate date = request.time().toLocalDate();

        CustomerLimit limit = limitRepository.findByCustomerIdAndCurrentDay(request.customerId(), date)
                .orElseGet(() -> createNewDayLimit(request.customerId(), date, request.time()));

        boolean accepted =
                limit.getDailyLoadCount() < maxFundsPerDay &&
                limit.getDailyTotalAmount().add(amount).compareTo(dailyMaxLimit) <= 0 &&
                limit.getWeeklyTotalAmount().add(amount).compareTo(weeklyMaxLimit) <= 0;

        if (accepted) {
            limit.setDailyLoadCount(limit.getDailyLoadCount() + 1);
            limit.setDailyTotalAmount(limit.getDailyTotalAmount().add(amount));
            limit.setWeeklyTotalAmount(limit.getWeeklyTotalAmount().add(amount));
            limitRepository.save(limit);
        }

        LoadFund fund = new LoadFund(request.id(), request.customerId(), amount, request.time(), accepted);
        loadFundRepository.save(fund);

        log.info("loading fund response: [id: {}, customerId: {}, accepted: {}]",
                request.id(),
                request.customerId(),
                accepted
        );

        return Optional.of(new LoadResponse(request.id(), request.customerId(), accepted));
    }

    private CustomerLimit createNewDayLimit(String customerId, LocalDate date, ZonedDateTime fundTime) {
        log.info("creating new daily limit entry for customer {} on date {}", customerId, date);
        CustomerLimit newLimit = new CustomerLimit();
        newLimit.setCustomerId(customerId);
        newLimit.setCurrentDay(date);
        newLimit.setDailyLoadCount(0);

        // daily amount always starts at zero for a new day
        newLimit.setDailyTotalAmount(BigDecimal.ZERO);

        // for the week, we need to figure when the previous monday was
        //   in the case that we are already on a monday, we will return the same day
        ZonedDateTime weekStartMonday= fundTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(fundTime.getZone());

        BigDecimal existingWeeklyTotal = loadFundRepository.sumAcceptedAmountsForCurrentWeek(
                customerId,
                weekStartMonday,
                fundTime
        );

        newLimit.setWeeklyTotalAmount(existingWeeklyTotal);

        return newLimit;
    }
}
