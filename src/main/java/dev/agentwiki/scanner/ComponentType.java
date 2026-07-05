package dev.agentwiki.scanner;

public enum ComponentType {
    REST_CONTROLLER("@RestController"),
    CONTROLLER("@Controller"),
    SERVICE("@Service"),
    COMPONENT("@Component"),
    REPOSITORY("@Repository"),
    ENTITY("@Entity"),
    CONFIGURATION("@Configuration"),
    CONFIGURATION_PROPERTIES("@ConfigurationProperties"),
    CONTROLLER_ADVICE("@ControllerAdvice"),
    EXCEPTION_HANDLER("@ExceptionHandler"),
    SCHEDULED("@Scheduled"),
    TRANSACTIONAL("@Transactional"),
    VALID("@Valid"),
    VALIDATED("@Validated");

    private final String annotation;

    ComponentType(String annotation) {
        this.annotation = annotation;
    }

    public String annotation() {
        return annotation;
    }
}
