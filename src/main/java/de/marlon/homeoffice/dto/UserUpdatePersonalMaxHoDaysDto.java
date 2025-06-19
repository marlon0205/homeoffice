package de.marlon.homeoffice.dto;

import lombok.Data;

// DTO für das Update des persönlichen max. HO-Tage-Limits eines Azubis
@Data
public class UserUpdatePersonalMaxHoDaysDto {
    // Integer, damit es null sein kann, falls der Wert zurückgesetzt wird (Gruppenstandard)
    private Integer personalMaxHoDaysPerWeek;
}