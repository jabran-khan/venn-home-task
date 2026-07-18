package com.jabrankhan.interview.venn_home_task.service;

import com.jabrankhan.interview.venn_home_task.dto.LoadRequest;
import com.jabrankhan.interview.venn_home_task.dto.LoadResponse;
import com.jabrankhan.interview.venn_home_task.model.CustomerLimit;
import com.jabrankhan.interview.venn_home_task.model.LoadFund;
import com.jabrankhan.interview.venn_home_task.repository.CustomerLimitRepository;
import com.jabrankhan.interview.venn_home_task.repository.LoadFundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LoadFundServiceTest {

    private LoadFundRepository loadFundRepository;
    private CustomerLimitRepository customerLimitRepository;
    private LoadFundService loadFundService;

    @BeforeEach
    void setUp() {
        loadFundRepository = Mockito.mock(LoadFundRepository.class);
        customerLimitRepository = Mockito.mock(CustomerLimitRepository.class);
        loadFundService = new LoadFundService(loadFundRepository, customerLimitRepository);

        ReflectionTestUtils.setField(loadFundService, "maxFundsPerDay", 3);
        ReflectionTestUtils.setField(loadFundService, "dailyMaxLimit", new BigDecimal("5000.00"));
        ReflectionTestUtils.setField(loadFundService, "weeklyMaxLimit", new BigDecimal("20000.00"));
    }

    @Test
    void shouldRejectDuplicateFundId() {
        LoadRequest request = new LoadRequest("123", "cust-1", "$100.00", ZonedDateTime.now());
        when(loadFundRepository.findByIdAndCustomerId("123", "cust-1")).thenReturn(Optional.of(new LoadFund()));

        Optional<LoadResponse> response = loadFundService.loadFunds(request);
        assertThat(response.isPresent()).isFalse();
        verifyNoInteractions(customerLimitRepository);
        verify(loadFundRepository, never()).save(any());
    }

    @Test
    void shouldAcceptFundWhenWithinDailyAndWeeklyLimits() {
        LoadRequest request = new LoadRequest("124", "cust-1", "$1500.00", ZonedDateTime.now());

        CustomerLimit existingLimit = new CustomerLimit();
        existingLimit.setCustomerId("cust-1");
        existingLimit.setDailyTotalAmount(new BigDecimal("1000.00"));
        existingLimit.setWeeklyTotalAmount(new BigDecimal("5000.00"));

        when(loadFundRepository.findById("124")).thenReturn(Optional.empty());
        when(customerLimitRepository.findByCustomerIdAndCurrentDay(eq("cust-1"), any())).thenReturn(Optional.of(existingLimit));

        Optional<LoadResponse> opt = loadFundService.loadFunds(request);
        assertThat(opt.isPresent()).isTrue();

        LoadResponse response = opt.get();

        assertThat(response.accepted()).isTrue();
        assertThat(existingLimit.getDailyTotalAmount()).isEqualByComparingTo("2500.00");
        assertThat(existingLimit.getWeeklyTotalAmount()).isEqualByComparingTo("6500.00");

        verify(customerLimitRepository).save(existingLimit);
        verify(loadFundRepository).save(any(LoadFund.class));
    }

    @Test
    void shouldRejectTransactionWhenExceedingDailyLimit() {
        LoadRequest request = new LoadRequest("125", "cust-1", "$4000.00", ZonedDateTime.now());

        CustomerLimit existingLimit = new CustomerLimit();
        existingLimit.setCustomerId("cust-1");
        existingLimit.setDailyTotalAmount(new BigDecimal("1500.00"));  // 1500 + 4000 = 5500 (> 5000)
        existingLimit.setWeeklyTotalAmount(new BigDecimal("5000.00"));

        when(loadFundRepository.findById("125")).thenReturn(Optional.empty());
        when(customerLimitRepository.findByCustomerIdAndCurrentDay(eq("cust-1"), any())).thenReturn(Optional.of(existingLimit));

        Optional<LoadResponse> opt = loadFundService.loadFunds(request);
        assertThat(opt.isPresent()).isTrue();
        LoadResponse response = opt.get();

        assertThat(response.accepted()).isFalse();

        verify(customerLimitRepository, never()).save(any());

        ArgumentCaptor<LoadFund> fundCaptor = ArgumentCaptor.forClass(LoadFund.class);
        verify(loadFundRepository).save(fundCaptor.capture());
        assertThat(fundCaptor.getValue().isAccepted()).isFalse();
    }

    @Test
    void shouldCreateNewDayLimitWithAggregatedWeeklyAmountIfNoDailyRecordExists() {
        ZonedDateTime fundTime = ZonedDateTime.parse("2026-07-15T10:00:00Z"); // A Wednesday
        LoadRequest request = new LoadRequest("126", "cust-1", "$500.00", fundTime);

        when(loadFundRepository.findById("126")).thenReturn(Optional.empty());
        // simulates that there was not database entry for this user today
        when(customerLimitRepository.findByCustomerIdAndCurrentDay(eq("cust-1"), any())).thenReturn(Optional.empty());
        // mocks the historical calculation for the week
        when(loadFundRepository.sumAcceptedAmountsForCurrentWeek(eq("cust-1"), any(), eq(fundTime)))
                .thenReturn(new BigDecimal("3000.00"));

        Optional<LoadResponse> opt = loadFundService.loadFunds(request);
        assertThat(opt.isPresent()).isTrue();
        LoadResponse response = opt.get();

        assertThat(response.accepted()).isTrue();

        // verify that new day limit record was created
        ArgumentCaptor<CustomerLimit> limitCaptor = ArgumentCaptor.forClass(CustomerLimit.class);
        verify(customerLimitRepository).save(limitCaptor.capture());

        CustomerLimit capturedLimit = limitCaptor.getValue();
        assertThat(capturedLimit.getDailyTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(capturedLimit.getWeeklyTotalAmount()).isEqualByComparingTo("3500.00");
    }

    @Test
    void shouldRejectTransactionWhenExceedingMaxDailyLoadCount() {
        LoadRequest request = new LoadRequest("201", "cust-99", "$1.00", ZonedDateTime.now());

        CustomerLimit existingLimit = new CustomerLimit();
        existingLimit.setCustomerId("cust-99");
        existingLimit.setDailyLoadCount(3); // Already at maximum allowed limit of 3
        existingLimit.setDailyTotalAmount(new BigDecimal("100.00"));
        existingLimit.setWeeklyTotalAmount(new BigDecimal("100.00"));

        when(loadFundRepository.findById("201")).thenReturn(Optional.empty());
        when(customerLimitRepository.findByCustomerIdAndCurrentDay(eq("cust-99"), any())).thenReturn(Optional.of(existingLimit));

        Optional<LoadResponse> opt = loadFundService.loadFunds(request);
        assertThat(opt.isPresent()).isTrue();
        LoadResponse response = opt.get();

        assertThat(response.accepted()).isFalse();
        verify(customerLimitRepository, never()).save(any());

        ArgumentCaptor<LoadFund> fundCaptor = ArgumentCaptor.forClass(LoadFund.class);
        verify(loadFundRepository).save(fundCaptor.capture());
        assertThat(fundCaptor.getValue().isAccepted()).isFalse();
    }
}
