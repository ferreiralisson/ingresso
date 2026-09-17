package org.example.ingresso.ingresso.config;

public final class Constants {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String SECURITY_ROLE_ADMIN = "hasAnyRole('ADMIN')";
    public static final String SECURITY_ROLE_USER = "hasAnyRole('ADMIN','USER')";

    private Constants() {
    }
}
