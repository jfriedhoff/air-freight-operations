# CI pipeline: the tests in GitHub Actions

Every pull request and every push to `main` runs all tests in GitHub Actions. The configuration is
[`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

## In short

- One job per service: `flight-operations`, `load-planning`, `messaging`, `simulator`. They run at
  the same time.
- Every job gets its own empty PostgreSQL and RabbitMQ (service containers), the same versions as
  `docker-compose.yml`. The simulator does not use its database.
- A job runs `./gradlew :<module>:test`: unit tests, MockMvc tests and both Cucumber suites.
  `@e2e` and `@ui` scenarios are not part of the pipeline: we run them by hand (see
  [`cucumber-acceptance-tests.md`](cucumber-acceptance-tests.md)). The logic behind the pages does run
  here, in the frontend jobs below.
- One job per frontend: `flight-operations-frontend`, `load-planning-frontend`. It first runs the unit tests
  with `node --test`, the test runner built into Node, so they need no JDK, Gradle, database or RabbitMQ.
  The tests only import our own code, so they need no `npm install`. Locally `npm test` in the
  frontend folder, or `./gradlew build`, runs them. See [`frontend-tests.md`](frontend-tests.md).
- The same job then type-checks the frontend with `tsc --noEmit`. `node --test` only strips the types, so a
  type error would still pass the tests; `tsc` checks them, as the local build does. This step needs the
  TypeScript compiler and the type packages, so it runs `npm ci` (without install scripts).

## Why one job per service

- Every context has its own database, in the pipeline as well.
- The integration tests send real RabbitMQ messages. With one shared RabbitMQ the tests of one
  service could consume the messages of another.
- The PostgreSQL image creates one database and one user from `POSTGRES_DB` and `POSTGRES_USER`.
  A service container cannot run `init-scripts/sql/init.sql`.

## How the tests find the database and RabbitMQ

The test profiles (`src/test/resources/application.properties`) read two variables with a fallback:

| Variable | In the pipeline | Without it (local) |
| --- | --- | --- |
| `CI_DB_HOST_PORT` | `localhost:5432` | `localhost:5443` |
| `CI_RABBIT_HOST` | `localhost` | `localhost` |

The jobs run on the runner itself, so the service containers are reachable on `localhost` through their
mapped ports. Locally nothing has to be set: the Docker Compose values apply. There is no Keycloak in the
pipeline; the tests use mocked JWTs and the service only contacts Keycloak for a real token.

## Where to look

- **Actions → CI**: the runs, their jobs and the logs.
- **Line coverage**: the percentage is in the summary of every service job. The full report is in the
  artifact `reports-<service>`: `build/reports/jacoco/test/html/index.html`, next to the Gradle test report
  and the JUnit XML files. Locally the same reports are written by every `./gradlew :<module>:test`.

## Speed

- `gradle/actions/setup-gradle` caches the Gradle distribution and the dependencies, `setup-node` the npm
  packages.
- A newer push cancels a run on the same branch that is still going (`concurrency`).

## Mutation testing

[`.github/workflows/mutation-tests.yml`](../.github/workflows/mutation-tests.yml) has one job per main
service (`pit-flight-operations`, `pit-load-planning`, `pit-messaging`). It only runs when started by hand
(Actions → Mutation tests → Run workflow), because PIT is slow. The report is in the artifact
`pitest-<service>`: `build/reports/pitest/index.html`. See [`mutation-testing-pit.md`](mutation-testing-pit.md).

## References

- [GitHub Actions — Service containers](https://docs.github.com/en/actions/use-cases-and-examples/using-containerized-services/about-service-containers)
  and [Creating PostgreSQL service containers](https://docs.github.com/en/actions/use-cases-and-examples/using-containerized-services/creating-postgresql-service-containers):
  a database next to a job, and how the job reaches it.
- [GitHub Actions — Workflow syntax](https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions):
  `services`, `strategy.matrix`, `concurrency`, `workflow_dispatch`.
- [gradle/actions — setup-gradle](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md): caching
  for Gradle builds.
- [JaCoCo — Documentation](https://www.jacoco.org/jacoco/trunk/doc/): the coverage report.
