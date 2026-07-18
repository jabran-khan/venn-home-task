package com.jabrankhan.interview.venn_home_task.service;

import com.jabrankhan.interview.venn_home_task.dto.LoadRequest;
import com.jabrankhan.interview.venn_home_task.dto.LoadResponse;
import com.jabrankhan.interview.venn_home_task.repository.CustomerLimitRepository;
import com.jabrankhan.interview.venn_home_task.repository.LoadFundRepository;
import org.springframework.stereotype.Service;

@Service
public class LoadFundService {

    private final LoadFundRepository loadFundsRepository;
    private final CustomerLimitRepository limitRepository;

    public LoadFundService(LoadFundRepository loadFundsProcessor, CustomerLimitRepository limitRepository) {
        this.loadFundsRepository = loadFundsProcessor;
        this.limitRepository = limitRepository;
    }

    public LoadResponse loadFunds(LoadRequest request) {
        return null;
    }
}
