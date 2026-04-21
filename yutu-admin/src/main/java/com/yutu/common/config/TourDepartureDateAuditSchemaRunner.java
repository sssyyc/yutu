package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourDepartureDateAuditSchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TourDepartureDateAuditSchemaRunner.class);
    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String TABLE_NAME = "tour_departure_date";
    private static final String AUDIT_STATUS_SQL =
            "ALTER TABLE tour_departure_date ADD COLUMN audit_status TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核 1通过 2驳回' AFTER status";
    private static final String AUDIT_REMARK_SQL =
            "ALTER TABLE tour_departure_date ADD COLUMN audit_remark VARCHAR(255) NULL AFTER audit_status";

    private final JdbcTemplate jdbcTemplate;

    public TourDepartureDateAuditSchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            log.info("Table {} does not exist, skipping column addition", TABLE_NAME);
            return;
        }
        ensureColumn("audit_status", AUDIT_STATUS_SQL);
        ensureColumn("audit_remark", AUDIT_REMARK_SQL);
    }

    private boolean tableExists() {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, TABLE_NAME);
            return tableCount != null && tableCount > 0;
        } catch (Exception ex) {
            log.warn("Failed to check if table {} exists", TABLE_NAME, ex);
            return false;
        }
    }

    private void ensureColumn(String columnName, String alterSql) {
        try {
            Integer columnCount = jdbcTemplate.queryForObject(COLUMN_EXISTS_SQL, Integer.class, TABLE_NAME, columnName);
            if (columnCount != null && columnCount > 0) {
                return;
            }
            jdbcTemplate.execute(alterSql);
            log.info("Added column {}.{} successfully", TABLE_NAME, columnName);
        } catch (Exception ex) {
            log.warn("Failed to ensure column {}.{} exists", TABLE_NAME, columnName, ex);
        }
    }
}
