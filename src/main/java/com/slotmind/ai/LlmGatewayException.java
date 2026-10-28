package com.slotmind.ai;

public class LlmGatewayException extends RuntimeException {
    public LlmGatewayException(String message) { super(message); }
    public LlmGatewayException(String message, Throwable cause) { super(message, cause); }
}