package com.larleeloo.jormungandr.cloud;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates player access codes. For testing, supports 25 hardcoded codes.
 * In production, codes are validated against the cloud.
 */
public class AccessCodeValidator {

    // 25 initial testing access codes
    private static final Set<String> VALID_CODES = new HashSet<>(Arrays.asList(
            "JORM-ALPHA-001", "JORM-ALPHA-002", "JORM-ALPHA-003", "JORM-ALPHA-004", "JORM-ALPHA-005",
            "JORM-ALPHA-006", "JORM-ALPHA-007", "JORM-ALPHA-008", "JORM-ALPHA-009", "JORM-ALPHA-010",
            "JORM-ALPHA-011", "JORM-ALPHA-012", "JORM-ALPHA-013", "JORM-ALPHA-014", "JORM-ALPHA-015",
            "JORM-ALPHA-016", "JORM-ALPHA-017", "JORM-ALPHA-018", "JORM-ALPHA-019", "JORM-ALPHA-020",
            "JORM-ALPHA-021", "JORM-ALPHA-022", "JORM-ALPHA-023", "JORM-ALPHA-024", "JORM-ALPHA-025"
    ));

    /**
     * Validate an access code locally. For testing, accepts the 25 predefined codes
     * plus any non-empty code (to allow easy testing during development).
     */
    public static boolean validateLocal(String code) {
        if (code == null || code.trim().isEmpty()) return false;
        return VALID_CODES.contains(code.toUpperCase());
    }

    /**
     * Check if a code is one of the official 25 testing codes.
     */
    public static boolean isOfficialCode(String code) {
        return code != null && VALID_CODES.contains(code.toUpperCase());
    }

    public static Set<String> getValidCodes() {
        return VALID_CODES;
    }
}
