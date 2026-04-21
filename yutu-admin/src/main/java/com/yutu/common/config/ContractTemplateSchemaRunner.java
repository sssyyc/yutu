package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContractTemplateSchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ContractTemplateSchemaRunner.class);
    private static final String TABLE_NAME = "contract_template";
    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";

    private static final String ADD_TEMPLATE_TYPE_SQL =
            "ALTER TABLE contract_template ADD COLUMN template_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD' AFTER template_code";
    private static final String ADD_APPLY_SCOPE_SQL =
            "ALTER TABLE contract_template ADD COLUMN apply_scope VARCHAR(32) NOT NULL DEFAULT 'DOMESTIC' AFTER template_type";
    private static final String ADD_DOWNLOAD_COUNT_SQL =
            "ALTER TABLE contract_template ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0 AFTER template_content";
    private static final String ADD_USE_COUNT_SQL =
            "ALTER TABLE contract_template ADD COLUMN use_count BIGINT NOT NULL DEFAULT 0 AFTER download_count";

    private final JdbcTemplate jdbcTemplate;

    public ContractTemplateSchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists()) {
            log.info("Table {} does not exist, skipping schema upgrade", TABLE_NAME);
            return;
        }
        ensureColumn("template_type", ADD_TEMPLATE_TYPE_SQL);
        ensureColumn("apply_scope", ADD_APPLY_SCOPE_SQL);
        ensureColumn("download_count", ADD_DOWNLOAD_COUNT_SQL);
        ensureColumn("use_count", ADD_USE_COUNT_SQL);
        normalizeExistingData();
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

    private void normalizeExistingData() {
        try {
            jdbcTemplate.execute(
                    "UPDATE contract_template " +
                            "SET template_type = COALESCE(NULLIF(template_type, ''), 'STANDARD'), " +
                            "apply_scope = COALESCE(NULLIF(apply_scope, ''), 'DOMESTIC'), " +
                            "download_count = IFNULL(download_count, 0), " +
                            "use_count = IFNULL(use_count, 0)"
            );
            log.info("Normalized existing data for {}", TABLE_NAME);
        } catch (Exception ex) {
            log.warn("Failed to normalize existing data for {}", TABLE_NAME, ex);
        }
    }
}
