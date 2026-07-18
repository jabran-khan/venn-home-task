package com.jabrankhan.interview.venn_home_task.service;

import com.jabrankhan.interview.venn_home_task.dto.FileStreamingSummary;
import com.jabrankhan.interview.venn_home_task.dto.LoadRequest;
import com.jabrankhan.interview.venn_home_task.dto.LoadResponse;
import com.jabrankhan.interview.venn_home_task.exception.FileReadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
@Service
public class FileStreamingService {

    private final LoadFundService loadFundService;
    private final ObjectMapper objectMapper;

    public FileStreamingService(LoadFundService loadFundService, ObjectMapper objectMapper) {
        this.loadFundService = loadFundService;
        this.objectMapper = objectMapper;
    }

    public FileStreamingSummary processFile(String inputFilePath, String outputFilePath) {
        Path inputPath = Paths.get(inputFilePath);
        Path outputPath = Paths.get(outputFilePath);

        int totalLines = 0;
        int success = 0;
        int rejected = 0;
        int errors = 0;

        log.info("Starting file processing for the input path: {}", inputFilePath);

        try (Stream<String> lines = Files.lines(inputPath);
             BufferedWriter writer = Files.newBufferedWriter(outputPath)) {

            var iterator = lines.iterator();
            while (iterator.hasNext()) {
                String line = iterator.next();
                if (line.trim().isEmpty()) continue;

                totalLines++;

                try {
                    LoadRequest request = objectMapper.readValue(line, LoadRequest.class);

                    LoadResponse response = loadFundService.loadFunds(request);
                    if (response.accepted()) {
                        success++;
                    } else {
                        rejected++;
                    }

                    String outputJson = objectMapper.writeValueAsString(response);
                    writer.write(outputJson);
                    writer.newLine();
                } catch (Exception e) {
                    errors++;
                    log.error("Error processing line {}: [{}]", totalLines, line, e);
                }
            }
        } catch (IOException e) {
            log.error("Failure in reading input file: {}", inputFilePath, e);
            throw new FileReadException("Failed to read the input file", e);
        }

        return new FileStreamingSummary(totalLines, success, rejected, errors);
    }
}
