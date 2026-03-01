package com.dahyvuun.payment_router.exception;

/**
 * 외부 API (환율 API, 결제 게이트웨이 등) 호출 실패 시 발생하는 예외
 *
 * Circuit Breaker가 이 예외를 실패로 카운트함 (application.yml의 record-exceptions 설정)
 */
public class ExternalApiException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    /**
     * HTTP 상태 코드가 있는 경우 (4xx, 5xx 응답)
     */
    public ExternalApiException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    /**
     * 연결 실패, 타임아웃 등 HTTP 상태 코드가 없는 경우
     */
    public ExternalApiException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = -1;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "ExternalApiException{" +
            "serviceName='" + serviceName + '\'' +
            ", statusCode=" + statusCode +
            ", message='" + getMessage() + '\'' +
            '}';
    }
}