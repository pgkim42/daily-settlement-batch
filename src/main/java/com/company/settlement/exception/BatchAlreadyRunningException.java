package com.company.settlement.exception;

/**
 * 배치가 이미 실행 중일 때 발생하는 예외
 */
public class BatchAlreadyRunningException extends RuntimeException {

    private final String jobName;

    public BatchAlreadyRunningException(String jobName) {
        super("Batch job is already running: " + jobName);
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
