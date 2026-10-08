package be.kdg.airfreight.flightoperations.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {
    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void everyRealmRoleBecomesAnAuthority() {
        Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("flight-planner", "offline_access"))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_flight-planner", "ROLE_offline_access");
    }

    @Test
    void aTokenWithoutRealmRolesHasNoAuthorities() {
        assertThat(converter.convert(token(Map.of("scope", "openid")))).isEmpty();
        assertThat(converter.convert(token(Map.of("realm_access", Map.of())))).isEmpty();
    }

    private static Jwt token(Map<String, Object> claims) {
        return Jwt.withTokenValue("token").header("alg", "none").claims(all -> all.putAll(claims)).build();
    }
}
