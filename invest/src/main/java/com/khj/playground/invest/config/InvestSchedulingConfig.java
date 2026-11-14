package com.khj.playground.invest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 보유 종목 시세 갱신(@Scheduled)을 위해 스케줄링을 켠다. 다른 모듈에는 스케줄 작업이 없다. */
@Configuration
@EnableScheduling
public class InvestSchedulingConfig {}
