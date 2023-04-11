package com.baracklee.rest.mq.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;

@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void registerReport() {
        registerMetricReport();
    }

    //if you have register Metrics
    private void registerMetricReport() {

    }

}
