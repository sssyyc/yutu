package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefundSchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RefundSchemaRunner.class);
    private static final String TABLE_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String REFUND_ORDER_TABLE = "refund_order";
    private static final String REFUND_FLOW_TABLE = "refund_flow";
    private static final String CREATE_REFUND_ORDER_SQL = "CREATE TABLE refund_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "refund_no VARCHAR(64) NOT NULL," +
            "order_id BIGINT NOT NULL," +
            "order_no VARCHAR(64) NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "merchant_id BIGINT NOT NULL," +
            "route_id BIGINT NULL," +
            "depart_date_id BIGINT NULL," +
            "refund_type VARCHAR(32) NOT NULL," +
            "refund_reason VARCHAR(500) NOT NULL," +
            "evidence_urls TEXT NULL," +
            "refund_account_type VARCHAR(32) NOT NULL," +
            "refund_account_no VARCHAR(128) NULL," +
            "original_order_status VARCHAR(32) NULL," +
            "original_pay_status VARCHAR(32) NULL," +
            "expected_refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
            "proposed_refund_amount DECIMAL(10,2) NULL," +
            "final_refund_amount DECIMAL(10,2) NULL," +
            "deduct_amount DECIMAL(10,2) NULL," +
            "fee_breakdown_json TEXT NULL," +
            "policy_note VARCHAR(500) NULL," +
            "merchant_note TEXT NULL," +
            "admin_note TEXT NULL," +
            "execution_note TEXT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "merchant_deadline_time DATETIME NULL," +
            "merchant_processed_time DATETIME NULL," +
            "admin_deadline_time DATETIME NULL," +
            "refund_processed_time DATETIME NULL," +
            "completed_time DATETIME NULL," +
            "deleted TINYINT NOT NULL DEFAULT 0," +
            "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "UNIQUE KEY uk_refund_order_refund_no (refund_no)," +
            "KEY idx_refund_order_order_id (order_id)," +
            "KEY idx_refund_order_user_id (user_id)," +
            "KEY idx_refund_order_merchant_id (merchant_id)," +
            "KEY idx_refund_order_status (status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    private static final String CREATE_REFUND_FLOW_SQL = "CREATE TABLE refund_flow (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "refund_id BIGINT NOT NULL," +
            "operator_id BIGINT NOT NULL," +
            "operator_role VARCHAR(32) NOT NULL," +
            "action_type VARCHAR(32) NOT NULL," +
            "action_content TEXT NULL," +
            "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "KEY idx_refund_flow_refund_id (refund_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private final JdbcTemplate jdbcTemplate;

    public RefundSchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable(REFUND_ORDER_TABLE, CREATE_REFUND_ORDER_SQL);
        ensureTable(REFUND_FLOW_TABLE, CREATE_REFUND_FLOW_SQL);
    }

    private void ensureTable(String tableName, String ddl) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(TABLE_EXISTS_SQL, Integer.class, tableName);
            if (tableCount != null && tableCount > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("Created {} table", tableName);
        } catch (Exception ex) {
            log.warn("Failed to ensure {} table exists", tableName, ex);
        }
    }
}
