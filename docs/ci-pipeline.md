# CI pipeline: the tests in GitLab

Every merge request and every push to `main` runs all tests in GitLab. The configuration is
`.gitlab-ci.yml` in the repository root. It follows the pattern of the course (slides 06,
"Database in de GitLab pipeline").

## In short

- One job per service: `flight-operations`, `load-planning`, `messaging`, `simulator`. They run at
  the same time.
- Every job gets its own empty PostgreSQL and RabbitMQ (`services`), the same versions as
  `docker-compose.yml`. The simulator job has no database.
- A job runs `./gradlew :<module>:test`: unit tests, MockMvc tests and both Cucumber suites.
  `@e2e` and `@ui` scenarios are not part of the pipeline: we run them by hand (see
  [`cucumber-acceptance-tests.md`](cucumber-acceptance-tests.md)). The logic behind the pages does run
  here, in the frontend jobs below.
- One job per frontend: `flight-operations-frontend`, `load-planning-frontend`. They run on a Node image with
  `node --test`, the test runner built into Node, so it needs no JDK, Gradle, database or RabbitMQ.
  The tests only import our own code, so there is no `npm install`. Locally `npm test` in the
  frontend folder, or `./gradlew build`, runs them. See [`frontend-tests.md`](frontend-tests.md).
- One type check per frontend: `flight-operations-frontend-typecheck`, `load-planning-frontend-typecheck`.
  `node --test` only strips the types, so a type error would still pass the job above; `tsc --noEmit` checks
  them, as the local build does. This job needs the TypeScript compiler and the type packages, so it runs
  `npm ci` (without install scripts, with an npm cache per job).

## Why one job per service

- Every context has its own database, in the pipeline as well.
- The integration tests send real RabbitMQ messages. With one shared RabbitMQ the tests of one
  service could consume the messages of another.
- The PostgreSQL image creates one database and one user from `POSTGRES_DB` and `POSTGRES_USER`.
  A job cannot mount `init-scripts/sql/init.sql`.

## How the tests find the database and RabbitMQ

The test profiles (`src/test/resources/application.properties`) read two variables with a fallback:

| Variable | In the pipeline | Without it (local) |
| --- | --- | --- |
| `CI_DB_HOST_PORT` | `postgres:5432` | `localhost:5443` |
| `CI_RABBIT_HOST` | `rabbitmq` | `localhost` |

Locally nothing has to be set: the Docker Compose values apply. There is no Keycloak in the
pipeline; the tests use mocked JWTs and the service only contacts Keycloak for a real token.

## Where to look

- **Build → Pipelines**: the jobs and their logs.
- **Tests tab** of the pipeline or the merge request: every test class and every Cucumber scenario.
  A scenario is reported by both suites (one runs it, the other filters it out by tag). GitLab
  counts a test name once, so it shows fewer tests than Gradle does; "passed" is the number of
  different tests that passed.
- **Coverage**: the percentage per job (Build → Jobs) and in the merge request. The full report is
  in the job artifacts: `build/reports/jacoco/test/html/index.html`. Locally the same report is
  written by every `./gradlew :<module>:test`.

## Speed

- The Gradle distribution and the dependencies are cached per job (`.gradle-home`).
- A newer push cancels a pipeline that is still running (`interruptible`).

## Mutation testing

The stage `mutation` has one job per main service (`pit-flight-operations`, `pit-load-planning`,
`pit-messaging`). They are manual: a job only runs when someone clicks its play button in the pipeline,
because PIT is slow. The report is in the job artifacts: `build/reports/pitest/index.html`. See
[`mutation-testing-pit.md`](mutation-testing-pit.md).

## References

- Course slides 06 (Spring testing), "Continuous Integration": "Configuratie met variabelen" and
  "Database in de GitLab pipeline".
- [GitLab — Services](https://docs.gitlab.com/ci/services/) and
  [Using PostgreSQL](https://docs.gitlab.com/ci/services/postgres/): a database next to a job, and
  the host name it gets.
- [GitLab — Unit test reports](https://docs.gitlab.com/ci/testing/unit_test_reports/): where the
  Tests tab gets its results from.
- [GitLab — Code coverage](https://docs.gitlab.com/ci/testing/code_coverage/): the percentage per
  job and in the merge request.
- [GitLab — CI/CD YAML syntax reference](https://docs.gitlab.com/ci/yaml/): `services`, `cache`,
  `interruptible`, `when: manual`.
- [JaCoCo — Documentation](https://www.jacoco.org/jacoco/trunk/doc/): the coverage report.
