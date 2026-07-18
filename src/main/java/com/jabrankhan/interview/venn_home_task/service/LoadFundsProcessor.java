package com.jabrankhan.interview.venn_home_task.service;

import org.springframework.stereotype.Service;

@Service
public class LoadFundsProcessor {

    private final LoadFundsRepository loadFundsRepository;
    private final CustomerLimitRepository limitRepository;

    public LoadFundsProcessor(LoadFundsRepository loadFundsProcessor, CustomerLimitRepository limitRepository) {
        this.loadFundsRepository = loadFundsProcessor;
        this.limitRepository = limitRepository;
    }

}
