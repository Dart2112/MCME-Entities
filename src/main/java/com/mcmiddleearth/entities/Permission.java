package com.mcmiddleearth.entities;

public enum Permission {

    USER    ("mcmeentities.user"),
    VIEWER  ("mcmeentities.viewer"),
    ADMIN   ("mcmeentities.admin");

    private final String node;

    Permission(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }
}
