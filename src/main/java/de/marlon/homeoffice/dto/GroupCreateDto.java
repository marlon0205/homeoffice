package de.marlon.homeoffice.dto;

import lombok.Data;

@Data
public class GroupCreateDto {
    private String name;
    private int maxHoDaysPerWeek;
}