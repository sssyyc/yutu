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
    private static final String LEGACY_UNIQUE_INDEX = "uk_contract_signature_contract";
    private static final String SIGNER_UNIQUE_INDEX = "uk_contract_signature_contract_traveler";
    private static final String TRAVELER_ID_COLUMN = "traveler_id";
    private static final String EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
    private static final String COLUMN_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
    private static final String INDEX_EXISTS_SQL =
            "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
    private static final String CREATE_SQL = "CREATE TABLE tour_contract_signature (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "contract_id BIGINT NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "traveler_id BIGINT NULL," +
            "signer_name VARCHAR(64) NOT NULL," +
            "signature_image LONGTEXT NOT NULL," +
            "sign_time DATETIME NOT NULL," +
            "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "UNIQUE KEY uk_contract_signature_contract_traveler (contract_id, traveler_id)" +
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
                ensureTravelerIdColumn();
                ensureSignatureIndexes();
                return;
            }
            jdbcTemplate.execute(CREATE_SQL);
            log.info("Created {} table", TABLE_NAME);
        } catch (Exception ex) {
            log.warn("Failed to ensure {} table exists", TABLE_NAME, ex);
        }
    }

    private void ensureTravelerIdColumn() {
        Integer columnCount = jdbcTemplate.queryForObject(COLUMN_EXISTS_SQL, Integer.class, TABLE_NAME, TRAVELER_ID_COLUMN);
        if (columnCount != null && columnCount > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE tour_contract_signature ADD COLUMN traveler_id BIGINT NULL AFTER user_id");
        log.info("Added traveler_id column to {}", TABLE_NAME);
    }

    private void ensureSignatureIndexes() {
        Integer legacyIndexCount = jdbcTemplate.queryForObject(INDEX_EXISTS_SQL, Integer.class, TABLE_NAME, LEGACY_UNIQUE_INDEX);
        if (legacyIndexCount != null && legacyIndexCount > 0) {
            jdbcTemplate.execute("ALTER TABLE tour_contract_signature DROP INDEX " + LEGACY_UNIQUE_INDEX);
            log.info("Dropped legacy index {} from {}", LEGACY_UNIQUE_INDEX, TABLE_NAME);
        }

        Integer signerIndexCount = jdbcTemplate.queryForObject(INDEX_EXISTS_SQL, Integer.class, TABLE_NAME, SIGNER_UNIQUE_INDEX);
        if (signerIndexCount != null && signerIndexCount > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE tour_contract_signature ADD UNIQUE KEY " + SIGNER_UNIQUE_INDEX + " (contract_id, traveler_id)");
        log.info("Added unique index {} to {}", SIGNER_UNIQUE_INDEX, TABLE_NAME);
    }
}
