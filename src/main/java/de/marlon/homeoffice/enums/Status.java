package de.marlon.homeoffice.enums;

public enum Status {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");

    private final String name;

    Status(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }



}
