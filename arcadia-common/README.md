# Arcadia common infrastructure classes

Java 21 / Spring Boot 3.3.4 source files for the `arcadia-common` module, based on the supplied parent and module POM files and the visible IDE screenshot.

## Contents

- `common.config`: async executor, OpenAPI metadata, Redis cache/template, security defaults, and MVC configuration.
- `common.error`: error codes, standardized error payload, and global exception handler.
- `common.error.exceptions`: typed application exceptions.
- `common.event`: domain event contract and trip-completed event.

## Integration notes

1. Copy `src/main/java/com/arcadia/common` into `arcadia-common/src/main/java/com/arcadia/common`.
2. The source uses dependencies declared in the supplied `arcadia-common/pom.xml` and Spring Boot dependency management from the parent POM; no additional dependency is required for these classes.
3. `SecurityConfig` avoids a compile-time dependency from `arcadia-common` to `arcadia-auth`. If the auth module provides a Spring `Filter` bean named `jwtAuthenticationFilter`, the shared security configuration adds it to the filter chain. Ensure the JWT filter is registered under that bean name (for example, with `@Component("jwtAuthenticationFilter")`).
4. The CORS origin `https://*.arcadia.example` is a placeholder. Replace it with the actual deployed frontend origin(s) before deployment.
5. `OpenApiConfig` sets common API metadata. It does not automatically aggregate separate YAML OpenAPI files; configure Springdoc groups/Swagger UI URLs if you need to expose multiple YAML specifications.
6. Redis uses JSON serialization for values and a five-minute default cache TTL. Command polling keys that require a 60-second TTL should set that TTL explicitly in the relevant operations cache/service.

These files are generated implementations based on the supplied architecture and class names, not recovered source from an existing repository. Compile them from the root Maven project with `./mvnw -pl arcadia-common -am clean compile`.
