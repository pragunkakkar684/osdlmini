package com.hotel.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates and normalizes room IDs.
 *
 * Accepted format:
 * - must start with R
 * - must be followed by digits only
 * - must not be all zeroes after the prefix
 * - leading/trailing spaces are trimmed
 * - lowercase ids are normalized to uppercase
 */
public final class RoomIdValidator {

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R[1-9][0-9]{2}$");

    private RoomIdValidator() {
    }

    public static String validate(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("Room ID cannot be blank.");
        }

        String normalized = roomId.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be blank.");
        }
        if (!ROOM_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Room ID must be in the form R### and cannot be 000, R000, or any other zero/invalid code.");
        }
        return normalized;
    }

    public static boolean isValid(String roomId) {
        try {
            validate(roomId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}