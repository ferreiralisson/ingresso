package org.example.ingresso.ingresso.model.enums;

public enum EntryCheckInOutcome {
    ACCEPTED,
    ALREADY_USED,
    INVALID,
    WRONG_EVENT,
    EVENT_UNAVAILABLE,
    OUTSIDE_EVENT_DAY,
    OFFLINE_CONFLICT,
    NOT_IN_OFFLINE_MANIFEST,
    MANIFEST_EXPIRED,
    REFUNDED,
    OPERATOR_UNAUTHORIZED
}