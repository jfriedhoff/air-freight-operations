CREATE USER flight_operations WITH PASSWORD 'password';
CREATE DATABASE flight_operations OWNER flight_operations;

CREATE USER load_planning WITH PASSWORD 'password';
CREATE DATABASE load_planning OWNER load_planning;

CREATE USER messaging WITH PASSWORD 'password';
CREATE DATABASE messaging OWNER messaging;

-- The integration tests of Flight Operations: never the data of a running service
CREATE DATABASE flight_operations_test OWNER flight_operations;
