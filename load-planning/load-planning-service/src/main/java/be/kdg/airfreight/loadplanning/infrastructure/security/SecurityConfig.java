package be.kdg.airfreight.loadplanning.infrastructure.security;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Who may reach the API. A user is either a flight planner or a load planner: both may read, only the
 * load planner may change the load, and a user with no planner role or with both gets no access.
 */
@Configuration
class SecurityConfig {
    private static final String FLIGHT_PLANNER = "ROLE_flight-planner";
    private static final String LOAD_PLANNER = "ROLE_load-planner";
    private static final Set<String> PLANNER_ROLES = Set.of(FLIGHT_PLANNER, LOAD_PLANNER);

    // The API needs a token; the page itself stays public, it sends the user to Keycloak.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(requests -> requests
                // Check Load only reads, but it is an action of the load planner (story 33)
                .requestMatchers(HttpMethod.GET, "/api/flight-cargos/*/load-check").access(onlyLoadPlanner())
                .requestMatchers(HttpMethod.GET, "/api/**").access(onePlannerRole())
                .requestMatchers("/api/**").access(onlyLoadPlanner())
                .anyRequest().permitAll())
            .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoles())))
            // no session and no cookie: every request proves itself with its token, so CSRF has nothing to forge
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> onePlannerRole() {
        return (authentication, request) -> new AuthorizationDecision(plannerRoles(authentication).size() == 1);
    }

    private static AuthorizationManager<RequestAuthorizationContext> onlyLoadPlanner() {
        return (authentication, request) ->
            new AuthorizationDecision(plannerRoles(authentication).equals(Set.of(LOAD_PLANNER)));
    }

    private static Set<String> plannerRoles(Supplier<? extends Authentication> authentication) {
        return authentication.get().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(PLANNER_ROLES::contains)
            .collect(Collectors.toSet());
    }

    private static JwtAuthenticationConverter keycloakRoles() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
