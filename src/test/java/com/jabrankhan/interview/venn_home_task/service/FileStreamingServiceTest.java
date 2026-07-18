package com.jabrankhan.interview.venn_home_task.service;

import com.jabrankhan.interview.venn_home_task.dto.FileStreamingSummary;
import com.jabrankhan.interview.venn_home_task.dto.LoadRequest;
import com.jabrankhan.interview.venn_home_task.dto.LoadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileStreamingServiceTest {

    private LoadFundService loadFundService;
    private ObjectMapper objectMapper;
    private FileStreamingService fileStreamingService;

    @BeforeEach
    void setup() {
        loadFundService = Mockito.mock(LoadFundService.class);
        objectMapper = new ObjectMapper();
        fileStreamingService = new FileStreamingService(loadFundService, objectMapper);
    }

    @Test
    void shouldSuccessfullyProcessFileWithMixOfResults(@TempDir Path tempDir) throws IOException {
        Path inputFilePath = tempDir.resolve("input.txt");
        Path outputFilePath = tempDir.resolve("ouput.txt");

        String line1 = "{\"id\":\"15887\",\"customer_id\":\"528\",\"load_amount\":\"$3318.47\",\"time\":\"2000-01-01T00:00:00Z\"}";
        String line2 = "{\"id\":\"15888\",\"customer_id\":\"742\",\"load_amount\":\"$1200.00\",\"time\":\"2000-01-01T01:15:00Z\"}";
        String line3 = "{\"id\":\"15888\",\"customer_id\":\"742\",\"load_amount\":\"$1200.00\",\"time\":\"2000-01-01T01:15:00Z\"}";
        Files.write(inputFilePath, List.of(line1, line2, line3));

        when(loadFundService.loadFunds(any(LoadRequest.class)))
                .thenReturn(Optional.of(new LoadResponse("15887", "528", true)))
                .thenReturn(Optional.of(new LoadResponse("15888", "742", false)))
                .thenReturn(Optional.empty());

        FileStreamingSummary summary = fileStreamingService.processFile(
                inputFilePath.toString(),
                outputFilePath.toString()
        );

        // verify that metrics are correct
        assertThat(summary.totalCount()).isEqualTo(3);
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.rejectCount()).isEqualTo(1);
        assertThat(summary.ignoredCount()).isEqualTo(1);
        assertThat(summary.errorCount()).isEqualTo(0);

        // capture and verify fields mapped for both records
        ArgumentCaptor<LoadRequest> requestCaptor = ArgumentCaptor.forClass(LoadRequest.class);
        verify(loadFundService, times(3)).loadFunds(requestCaptor.capture());

        List<LoadRequest> capturedRequests = requestCaptor.getAllValues();

        assertThat(capturedRequests.getFirst().id()).isEqualTo("15887");
        assertThat(capturedRequests.getFirst().customerId()).isEqualTo("528");
        assertThat(capturedRequests.getFirst().loadAmountStr()).isEqualTo("$3318.47");

        assertThat(capturedRequests.get(1).id()).isEqualTo("15888");
        assertThat(capturedRequests.get(1).customerId()).isEqualTo("742");
        assertThat(capturedRequests.get(1).loadAmountStr()).isEqualTo("$1200.00");

        assertThat(capturedRequests.get(2).id()).isEqualTo("15888");
        assertThat(capturedRequests.get(2).customerId()).isEqualTo("742");
        assertThat(capturedRequests.get(2).loadAmountStr()).isEqualTo("$1200.00");

        // verify the output file contains exactly 2 streamed JSON objects
        List<String> outputLines = Files.readAllLines(outputFilePath);
        assertThat(outputLines).hasSize(2);
        assertThat(outputLines.get(0)).contains("\"accepted\":true");
        assertThat(outputLines.get(1)).contains("\"accepted\":false");
    }
}
