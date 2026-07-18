package com.jabrankhan.interview.venn_home_task;

import com.jabrankhan.interview.venn_home_task.service.FileStreamingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class VennHomeTaskApplication implements CommandLineRunner {

	private final FileStreamingService fileStreamingService;

	public VennHomeTaskApplication(FileStreamingService fileStreamingService) {
		this.fileStreamingService = fileStreamingService;
	}

	static void main(String[] args) {
		SpringApplication.run(VennHomeTaskApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length < 2) {
			log.error("Not enough arguments provided. Please provide an input and output file path.");
			return;
		}

		String inputPath = args[0];
		String outputPath = args[1];

		log.info("Processing file");
		try {
			fileStreamingService.processFile(inputPath, outputPath);
			log.info("Successfully processed files");
		} catch (Exception e) {
			log.error("Error processing file", e);
		}
	}
}
