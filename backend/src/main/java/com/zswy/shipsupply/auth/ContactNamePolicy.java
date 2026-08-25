package com.zswy.shipsupply.auth;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ContactNamePolicy {

    private static final Pattern FAKE_PATH = Pattern.compile("(?:^|[\\\\/])fakepath[\\\\/]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[a-z]:[\\\\/].+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNC_PATH = Pattern.compile("^\\\\\\\\.+");
    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("^/.+");

    private ContactNamePolicy() {
    }

    static String requireValid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        String trimmed = value.trim();
        rejectPathValue(trimmed);
        return trimmed;
    }

    static void rejectPathValue(String value) {
        if (looksLikeFilePath(value)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "CONTACT_NAME_INVALID: contact name must not be a file path"
            );
        }
    }

    static boolean looksLikeFilePath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        return FAKE_PATH.matcher(trimmed).find()
            || WINDOWS_ABSOLUTE_PATH.matcher(trimmed).matches()
            || UNC_PATH.matcher(trimmed).matches()
            || UNIX_ABSOLUTE_PATH.matcher(trimmed).matches();
    }
}
