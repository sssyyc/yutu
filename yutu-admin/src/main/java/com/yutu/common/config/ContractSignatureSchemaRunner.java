package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContractSignatureSchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ContractSignatureSchemaRunner.class);
    private static final String TABLE_NAME = "tour_contract_signature";
    private static final String EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String CREATE_SQL = "CREATE TABLE tour_contract_signature (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "contract_id BIGINT NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "signer_name VARCHAR(64) NOT NULL," +
            "signature_image LONGTEXT NOT NULL," +
            "sign_time DATETIME NOT NULL," +
            "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "UNIQUE KEY uk_contract_signature_contract (contract_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private final JdbcTemplate jdbcTemplate;

    public ContractSignatureSchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(EXISTS_SQL, Integer.class, TABLE_NAME);
            if (tableCount != null && tableCount > 0) {
                return;
            }
            jdbcTemplate.execute(CREATE_SQL);
            log.info("Created {} table", TABLE_NAME);
        } catch (Exception ex) {
            log.warn("Failed to ensure {} table exists", TABLE_NAME, ex);
        }
    }
}