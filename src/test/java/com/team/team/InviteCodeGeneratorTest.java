package com.team.team;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteCodeGeneratorTest {

    private final InviteCodeGenerator generator = new InviteCodeGenerator();

    @Test
    void generate_producesEightCharactersWithoutAmbiguousCharacters() {
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String code = generator.generate();
            assertEquals(8, code.length());
            assertFalse(code.contains("0"));
            assertFalse(code.contains("O"));
            assertFalse(code.contains("1"));
            assertFalse(code.contains("I"));
            assertTrue(code.matches("[A-Z2-9]{8}"));
            codes.add(code);
        }

        assertTrue(codes.size() > 90);
    }
}
