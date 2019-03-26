package com.sappenin.interledger.ilpv4.connector.server.spring.settings;

import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableHystrix
public class ResiliencyConfig {
}
