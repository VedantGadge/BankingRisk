package com.example.bankingproject.common.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicator that goes beyond the default Spring DB check.
 * Reports:
 * - Whether a real connection can be acquired from the pool
 * - Current pool stats (active, idle, pending threads, max size)
 *
 * Visible at GET /actuator/health
 */
@Component("database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2); // 2-second timeout

            if (!valid) {
                return Health.down()
                        .withDetail("error", "Connection validation failed")
                        .build();
            }

            Health.Builder builder = Health.up()
                    .withDetail("database", connection.getMetaData().getDatabaseProductName())
                    .withDetail("status", "Connection acquired successfully");

            // Add HikariCP pool stats if available
            if (dataSource instanceof HikariDataSource hikari) {
                var pool = hikari.getHikariPoolMXBean();
                if (pool != null) {
                    builder
                            .withDetail("pool.active", pool.getActiveConnections())
                            .withDetail("pool.idle", pool.getIdleConnections())
                            .withDetail("pool.total", pool.getTotalConnections())
                            .withDetail("pool.awaiting", pool.getThreadsAwaitingConnection())
                            .withDetail("pool.max", hikari.getMaximumPoolSize());
                }
            }

            return builder.build();

        } catch (Exception e) {
            log.error("[health] database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
