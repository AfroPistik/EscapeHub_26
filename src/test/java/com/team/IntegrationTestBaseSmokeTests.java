package com.team;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationTestBaseSmokeTests extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void databaseIsEmptyAtStartOfEachTest() {
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM teams", Integer.class));
    }
}