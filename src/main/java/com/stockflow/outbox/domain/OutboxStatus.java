package com.stockflow.outbox.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
