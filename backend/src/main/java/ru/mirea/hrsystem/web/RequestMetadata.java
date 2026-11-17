package ru.mirea.hrsystem.web;

import org.slf4j.MDC;

public final class RequestMetadata {
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private RequestMetadata() {}

    public static String correlationId() {
        String value = MDC.get(MDC_KEY);
        return value == null ? "" : value;
    }
}
