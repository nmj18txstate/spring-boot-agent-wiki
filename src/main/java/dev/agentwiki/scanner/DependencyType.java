package dev.agentwiki.scanner;

public enum DependencyType {
    SPRING_WEB("spring-boot-starter-web"),
    SPRING_DATA_JPA("spring-boot-starter-data-jpa"),
    SPRING_ACTUATOR("spring-boot-starter-actuator"),
    SPRING_VALIDATION("spring-boot-starter-validation"),
    SPRING_SECURITY("spring-boot-starter-security"),
    SPRING_TEST("spring-boot-starter-test"),
    TESTCONTAINERS("testcontainers"),
    FLYWAY("flyway"),
    LIQUIBASE("liquibase"),
    POSTGRESQL("postgresql"),
    KAFKA("kafka"),
    RABBITMQ("rabbitmq"),
    RESILIENCE4J("resilience4j");

    private final String marker;

    DependencyType(String marker) {
        this.marker = marker;
    }

    public String marker() {
        return marker;
    }
}
