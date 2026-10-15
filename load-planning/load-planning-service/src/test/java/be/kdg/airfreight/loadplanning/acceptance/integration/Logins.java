package be.kdg.airfreight.loadplanning.acceptance.integration;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Logging in happens at Keycloak; a request of a scenario arrives with the token instead. */
final class Logins {
    private Logins() {
    }

    /** A token for this account with these roles, written as in Keycloak ("load-planner"). */
    static RequestPostProcessor token(UUID subject, String... roles) {
        SimpleGrantedAuthority[] authorities = Arrays.stream(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toArray(SimpleGrantedAuthority[]::new);
        return jwt().jwt(token -> token.subject(subject.toString())).authorities(authorities);
    }

    /** A token that also carries the name Keycloak gives the account, as a real token does. */
    static RequestPostProcessor namedToken(UUID subject, String name, String... roles) {
        SimpleGrantedAuthority[] authorities = Arrays.stream(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toArray(SimpleGrantedAuthority[]::new);
        return jwt().jwt(token -> token.subject(subject.toString()).claim("name", name)).authorities(authorities);
    }
}
