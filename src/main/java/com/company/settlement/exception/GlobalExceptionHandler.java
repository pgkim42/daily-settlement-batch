package com.company.settlement.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * 전역 예외 처리 핸들러
 * RFC 7807 Problem Details 형식으로 응답
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SettlementNotFoundException.class)
    public ProblemDetail handleSettlementNotFound(SettlementNotFoundException ex) {
        log.warn("Settlement not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setTitle("Settlement Not Found");
        problem.setType(URI.create("https://api.company.com/errors/settlement-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("settlementId", ex.getSettlementId());

        return problem;
    }

    @ExceptionHandler(SettlementAccessDeniedException.class)
    public ProblemDetail handleSettlementAccessDenied(SettlementAccessDeniedException ex) {
        log.warn("Settlement access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://api.company.com/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(BatchAlreadyRunningException.class)
    public ProblemDetail handleBatchAlreadyRunning(BatchAlreadyRunningException ex) {
        log.warn("Batch already running: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setTitle("Batch Already Running");
        problem.setType(URI.create("https://api.company.com/errors/batch-already-running"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("jobName", ex.getJobName());

        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://api.company.com/errors/invalid-request"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.company.com/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}
