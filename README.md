# Queue Performance Benchmarking

This project is designed to benchmark the performance of different queue systems, namely Redis (with RDB and AOF configurations) and Beanstalkd. The benchmarking is performed using a custom Scala application within a Dockerized environment.

## Setup

The project uses Docker Compose to manage and orchestrate multiple services, including the queue systems and the benchmarking application. The `docker-compose.yml` file defines the necessary services:

- `redis-rdb`: Redis configured for RDB persistence.
- `redis-aof`: Redis configured for AOF persistence.
- `beanstalkd`: Beanstalkd queue system.
- `benchmark`: Custom Scala application for performing benchmarks.

## Running Benchmarks

To execute a benchmark, use the `docker-compose run` command with specific environment variables to target the desired queue system. For example:

1. This command runs the benchmark against the `redis-rdb` service:
    ```bash
    docker-compose run -e HOSTNAME=redis-rdb -e PORT=6379 -e NUMBER_OF_OPERATIONS=1000000 benchmark
    ```
2. This command runs the benchmark against the `redis-aof` service:
    ```bash
    docker-compose run -e HOSTNAME=redis-aof -e PORT=6379 -e NUMBER_OF_OPERATIONS=1000000 benchmark
    ```
3. This command runs the benchmark against the `beanstalkd` service:
    ```bash
    docker-compose run -e QUEUE_TYPE=beanstalkd -e HOSTNAME=beanstalkd -e PORT=11300 -e NUMBER_OF_OPERATIONS=100000 benchmark
    ```

### Benchmark Results

#### Redis (RDB)

- Total duration: `77.976 s`
- Total number of operations: `1,000,000`
- Operations per second: `12,824 op/s`

#### Redis (AOF)

- Total duration: `83.113 s`
- Total number of operations: `1,000,000`
- Operations per second: `12,031 op/s`

### Beanstalkd Results

- Total duration: `17.816 s`
- Total number of operations: `100,000`
- Operations per second: `5,612 op/s`

## Further Information

- The benchmarking application is written in Scala and packaged into a Docker image using Scala CLI.
- The application measures the total duration and calculates the operations per second for each benchmark.
- Profiles are used in Docker Compose to manage the benchmark service independently of other services.
