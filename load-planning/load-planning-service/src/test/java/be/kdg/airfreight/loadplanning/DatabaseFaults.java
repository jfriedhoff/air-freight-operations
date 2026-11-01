package be.kdg.airfreight.loadplanning;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

/**
 * Story #39: makes the database unreachable, or makes its operations fail, for one scenario. The DataSource of the
 * application is wrapped once at start-up; while nothing is broken the wrapper only passes everything on. A step
 * that breaks the database restores it after the scenario.
 * The database is outside our code, so it is broken at the edge with JDBC, where the driver would fail too: the
 * repositories, Hibernate and Spring's translation of the error run as in production.
 */
@Component
public class DatabaseFaults implements BeanPostProcessor {
    private static final Set<String> STATEMENTS = Set.of("prepareStatement", "createStatement", "prepareCall");

    private enum State { REACHABLE, UNREACHABLE, FAILING }

    private volatile State state = State.REACHABLE;

    public void makeUnreachable() {
        state = State.UNREACHABLE;
    }

    public void makeOperationsFail() {
        state = State.FAILING;
    }

    public void restore() {
        state = State.REACHABLE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean instanceof DataSource dataSource ? new FaultyDataSource(dataSource) : bean;
    }

    private class FaultyDataSource extends DelegatingDataSource {
        FaultyDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return withFaults(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return withFaults(super.getConnection(username, password));
        }

        private Connection withFaults(Connection connection) throws SQLException {
            if (state == State.UNREACHABLE) {
                connection.close();
                // 08001: the SQL state a driver gives when it cannot connect
                throw new SQLException("Connection refused: the database cannot be reached", "08001");
            }
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, arguments) -> {
                    if (state == State.FAILING && STATEMENTS.contains(method.getName())) {
                        // XX000: an internal error of PostgreSQL, one that Spring cannot place more precisely
                        throw new SQLException("Unexpected database failure", "XX000");
                    }
                    try {
                        return method.invoke(connection, arguments);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        }
    }
}
