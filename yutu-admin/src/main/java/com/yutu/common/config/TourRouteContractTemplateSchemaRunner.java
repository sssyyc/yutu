package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourRouteContractTemplateSchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TourRouteContractTemplateSchemaRunner.class);
    private static final String TABLE_NAME = "tour_route";
    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
    private static final String ADD_STANDARD_TEMPLATE_ID_SQL =
            "ALTER TABLE tour_route ADD COLUMN standard_template_id BIGINT NULL AFTER category_id";
    private static final String ADD_ROUTE_TEMPLATE_ID_SQL =
            "ALTER TABLE tour_route ADD COLUMN route_template_id BIGINT NULL AFTER standard_template_id";
    private static final String ADD_SUPPLEMENT_TEMPLATE_ID_SQL =
            "ALTER TABLE tour_route ADD COLUMN supplement_template_id BIGINT NULL AFTER route_template_id";

    private final JdbcTemplate jdbcTemplate;

    public TourRouteContractTemplateSchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            log.info("Table {} does not exist, skipping contract template column addition", TABLE_NAME);
            return;
        }
        ensureColumn("standard_template_id", ADD_STANDARD_TEMPLATE_ID_SQL);
        ensureColumn("route_template_id", ADD_ROUTE_TEMPLATE_ID_SQL);
        ensureColumn("supplement_template_id", ADD_SUPPLEMENT_TEMPLATE_ID_SQL);
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
