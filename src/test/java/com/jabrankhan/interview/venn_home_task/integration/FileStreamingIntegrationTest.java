package com.jabrankhan.interview.venn_home_task.integration;

import com.jabrankhan.interview.venn_home_task.dto.FileStreamingSummary;
import com.jabrankhan.interview.venn_home_task.model.CustomerLimit;
import com.jabrankhan.interview.venn_home_task.model.LoadFund;
import com.jabrankhan.interview.venn_home_task.repository.CustomerLimitRepository;
import com.jabrankhan.interview.venn_home_task.repository.LoadFundRepository;
import com.jabrankhan.interview.venn_home_task.service.FileStreamingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class FileStreamingIntegrationTest {

    @Autowired
    private FileStreamingService fileStreamingService;

    @Autowired
    private LoadFundRepository loadFundRepository;

    @Autowired
    private CustomerLimitRepository customerLimitRepository;

    @Test
    void shouldStreamFileAndPersistStates(@TempDir Path tempDir) throws IOException {
        List<String> inputLines = List.of(
                "{\"id\":\"1001\",\"customer_id\":\"888\",\"load_amount\":\"$1000.00\",\"time\":\"2026-07-18T01:00:00Z\"}",
                "{\"id\":\"1002\",\"customer_id\":\"888\",\"load_amount\":\"$3000.00\",\"time\":\"2026-07-18T02:00:00Z\"}",
                "{\"id\":\"1002\",\"customer_id\":\"888\",\"load_amount\":\"$3000.00\",\"time\":\"2026-07-18T02:00:00Z\"}", // purposefully duplicate
                "{\"id\":\"1003\",\"customer_id\":\"888\",\"load_amount\":\"$1500.00\",\"time\":\"2026-07-18T04:00:00Z\"}",
                "{\"id\":\"1004\",\"customer_id\":\"888\",\"load_amount\":\"$5.00\",\"time\":\"2026-07-18T05:00:00Z\"}",
                "{\"id\":\"1005\",\"customer_id\":\"888\",\"load_amount\":\"$5.00\",\"time\":\"2026-07-18T06:00:00Z\"}"
        );

        Path inputFilePath = tempDir.resolve("input.txt");
        Path outputFilePath = tempDir.resolve("output.txt");
        Files.write(inputFilePath, inputLines);

        FileStreamingSummary summary = fileStreamingService.processFile(
                inputFilePath.toString(),
                outputFilePath.toString()
        );

        assertThat(summary.totalCount()).isEqualTo(6);
        assertThat(summary.successCount()).isEqualTo(3);
        assertThat(summary.ignoredCount()).isEqualTo(1);
        assertThat(summary.rejectCount()).isEqualTo(2);
        assertThat(summary.errorCount()).isEqualTo(0);

        List<LoadFund> savedFunds = loadFundRepository.findAll();
        assertThat(savedFunds).hasSize(5);
        assertThat(savedFunds.stream().filter(LoadFund::isAccepted).count()).isEqualTo(3);

        CustomerLimit customerLimit = customerLimitRepository.findAll().stream()
                .filter(l -> l.getCustomerId().equals("888"))
                .findFirst()
                .orElseThrow();

        assertThat(customerLimit.getDailyLoadCount()).isEqualTo(3); // success increment only
        assertThat(customerLimit.getDailyTotalAmount()).isEqualByComparingTo("4005.00");

        List<String> outputLines = Files.readAllLines(outputFilePath);
        assertThat(outputLines).hasSize(5);
        assertThat(outputLines.get(0)).contains("\"accepted\":true");
        assertThat(outputLines.get(1)).contains("\"accepted\":true");
        assertThat(outputLines.get(2)).contains("\"accepted\":false");
        assertThat(outputLines.get(3)).contains("\"accepted\":true");
        assertThat(outputLines.get(4)).contains("\"accepted\":false");
    }
}
