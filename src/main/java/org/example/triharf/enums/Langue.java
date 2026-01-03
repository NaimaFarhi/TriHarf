package org.example.triharf.enums;

public enum Langue {
    FRANCAIS("fr"),
    ENGLISH("en"),
    ARABE("ar");

    private String code;

    Langue(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}