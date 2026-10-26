package edu.univ.erp.service;

/** Input checks shared by the services. */
final class Validate {

    private Validate() {
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new ServiceException(field + " is required.");
        return value.trim();
    }

    static String trimToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    static void email(String email) {
        if (email != null && !email.isBlank() && !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ServiceException("That email address doesn't look right.");
        }
    }

    static void username(String username) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new ServiceException("Username must be 3–50 characters: letters, numbers, dot, dash or underscore.");
        }
    }

    static int range(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new ServiceException(field + " must be between " + min + " and " + max + ".");
        }
        return value;
    }
}
