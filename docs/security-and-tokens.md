# Security: login, tokens and where they live

Users log in with Keycloak (realm `air-freight`). The frontend gets tokens from Keycloak and sends the
access token with every API call. The service checks that token on every request and keeps no session.
The setup follows the reference project (`ordering-system`, branch `6-security`); the differences are
listed at the end.

## In short

- Only **flight-operations** is secured. Load-planning, messaging and the simulator have no Spring
  Security yet; the realm already has the clients for load-planning.
- A token is **never written to disk** by our code: not in `localStorage`, not in a cookie, not in a
  database.
- The login session itself belongs to Keycloak, not to our application.

## Where is a token?

| Place | What is there | How long |
| --- | --- | --- |
| Browser tab (`keycloak-js` in `auth.ts`) | Access, refresh and ID token, in memory | Until reload or closing the tab |
| Keycloak (`localhost:8180`) | The login session: its own SSO cookie in the browser and the session in its MySQL database (`idp_mysql_data/`) | Until logout or the session expires |
| `flight-operations-service` | Nothing. It reads the token from the `Authorization` header of the request | Only during the request |
| IntelliJ HTTP client | The access token in the global variable `access_token` (`flight_api.http`) | Until IntelliJ closes |

After a reload the page has no token. It redirects to Keycloak, which still knows the session and
sends the browser back with a new code, so the user does not type the password again.

The realm file sets no lifetimes, so the Keycloak defaults apply (a short-lived access token that the
frontend refreshes).

## The login flow

1. The page loads and `keycloak.init` runs with `onLoad: "login-required"`. Without a session the
   browser goes to the Keycloak login page.
2. Keycloak sends the browser back with a code. `keycloak-js` exchanges it for tokens
   (authorization code flow with PKCE, `S256`). The frontend is a public client: a browser cannot
   keep a secret.
3. Every API call goes through `authorizedFetch`. It refreshes the access token when it expires
   within 30 seconds and adds `Authorization: Bearer <token>`.
4. The service validates the token: signature (public keys of the issuer), issuer
   (`KEYCLOAK_ISSUER_URI`) and audience (`flight-operations-service`). The audience is added to the
   token by a mapper on the client in the realm.
5. `KeycloakRealmRoleConverter` turns `realm_access.roles` into `ROLE_<role>` authorities.

The body of the page starts invisible and is shown after login, so nothing flashes before the redirect.

## Who may do what

Configured in `SecurityConfig`:

| Request | Allowed for |
| --- | --- |
| `GET /api/**` | A user with exactly one planner role (`flight-planner` or `load-planner`) |
| Other methods on `/api/**` | Only a `flight-planner` |
| Everything else (the page, JS, CSS) | Everyone; the page sends the user to Keycloak itself |

- No token gives 401, the wrong role gives 403.
- A user with both planner roles gets no access.
- `hasRole` in the frontend only decides which buttons are shown. The server checks again.
- `FlightController` takes the subject of the token (`jwt.getSubject()`) as the id of the flight
  planner who enters a delay. The planners in `flight-operations.planners` use the id of their
  Keycloak account for that reason.
- No session and no cookie, so CSRF protection is off: there is nothing a forged request could reuse.
- No CORS configuration: in development Vite forwards `/api` to the service, and the built frontend is
  served by the service itself.

## Secrets

The secrets of the realm and of the HTTP client are not in the repository:

| What | Where |
| --- | --- |
| Client secrets of `flight-operations-service` and `load-planning-service` | `.env` (ignored by git), handed to Keycloak by Docker Compose |
| Passwords of the two test users | `.env` |
| Client secret and passwords for the HTTP client | `http/http-client.private.env.json` of each service (ignored by git) |

`keycloak/air-freight-realm.json` holds a placeholder such as `${LOAD_PLANNER_PASSWORD}` for each of them;
Keycloak fills the placeholders from its environment when it imports the realm. `.env.example` lists the
names. The values that were committed before are still in the history of the repository and have been
replaced.

These development values are still committed:

| What | Where |
| --- | --- |
| Passwords of PostgreSQL, RabbitMQ, MySQL and the Keycloak admin | `docker-compose.yml`, `init-scripts/sql/init.sql`, `application.properties` |

The client secrets are only used by the password grant in the `.http` files. The frontends and the
services do not need them: a frontend is a public client and a service only validates tokens.

## Tests

The tests need no Keycloak. They use mocked JWTs (`spring-security-test`); the service only contacts
Keycloak when a real token arrives. See `SecurityConfigTest`, `KeycloakRealmRoleConverterTest` and
the controller tests.

## Compared with the reference project

The same: `auth.ts` (tokens in memory, PKCE, `authorizedFetch`), `userPresenter.ts`, the stateless
resource server with issuer and audience, `KeycloakRealmRoleConverter`, the login request in the
`.http` file.

| Topic | Reference project | This project |
| --- | --- | --- |
| Secrets for the HTTP client | In `http-client.private.env.json`, ignored by git | The same |
| Realm | Created by hand with `keycloak_setup.md`; no secret in the repository | Imported from `keycloak/air-freight-realm.json`; its secrets are placeholders filled from `.env` |
| Secured services | All three | Only flight-operations |
| Token between services | The order service gets a client credentials token to call the product service | None: our services only exchange RabbitMQ messages |
| Roles | `hasAnyRole("customer", "admin")` on `/api/**` | Exactly one planner role to read, `flight-planner` to change |
| Principal | `preferred_username` | The subject, used as flight planner id |
| CORS | `.cors(Customizer.withDefaults())` | Not needed, see above |

## References

- Reference project `ordering-system`, branch `6-security`: `auth.ts`, `SecurityConfig`,
  `KeycloakRealmRoleConverter`.
- [Keycloak — JavaScript adapter](https://www.keycloak.org/securing-apps/javascript-adapter):
  `keycloak-js`, `login-required`, PKCE and refreshing the token.
- [Spring Security — OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html):
  validating the signature, issuer and audience, and turning claims into authorities.
- [Spring Security — CSRF](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html):
  when CSRF protection is needed.
- [Spring Security — Testing OAuth 2.0 with MockMvc](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html):
  the mocked JWTs in the tests.
- [RFC 7636 — Proof Key for Code Exchange](https://datatracker.ietf.org/doc/html/rfc7636): PKCE.
- [OAuth 2.0 for Browser-Based Applications](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps):
  why a browser application is a public client and where it may keep tokens.
