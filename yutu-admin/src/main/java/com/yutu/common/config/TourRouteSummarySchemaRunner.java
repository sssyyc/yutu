package com.yutu.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TourRouteSummarySchemaRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TourRouteSummarySchemaRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public TourRouteSummarySchemaRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!isTourRouteSummaryVarchar()) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE tour_route MODIFY COLUMN summary TEXT NULL");
        log.info("Expanded tour_route.summary to TEXT");
    }

    private boolean isTourRouteSummaryVarchar() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ? AND data_type = ?",
                Integer.class,
                "tour_route",
                "summary",
                "varchar");
        return count != null && count > 0;
    }
}
