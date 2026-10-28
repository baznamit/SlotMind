package com.slotmind.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "slotmind.llm")
public record LlmProperties(String endpoint, String model, String product, String apiKey, Duration timeout) { }