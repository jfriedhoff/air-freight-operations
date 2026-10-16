CREATE USER flight_operations WITH PASSWORD 'password';
CREATE DATABASE flight_operations OWNER flight_operations;

CREATE USER load_planning WITH PASSWORD 'password';
CREATE DATABASE load_planning OWNER load_planning;

CREATE USER messaging WITH PASSWORD 'password';
CREATE DATABASE messaging OWNER messaging;

-- The integration tests of every service get their own database, so they never touch the data of a running service
CREATE DATABASE flight_operations_test OWNER flight_operations;
CREATE DATABASE load_planning_test OWNER load_planning;
CREATE DATABASE messaging_test OWNER messaging;
