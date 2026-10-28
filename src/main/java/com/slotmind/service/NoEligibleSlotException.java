package com.slotmind.service;

public class NoEligibleSlotException extends RuntimeException {
    public NoEligibleSlotException(String message) { super(message); }
}