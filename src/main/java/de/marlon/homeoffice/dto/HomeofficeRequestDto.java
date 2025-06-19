package de.marlon.homeoffice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data; // Lombok: Generiert Getter, Setter, toString, equals, hashCode

import java.time.LocalDate;

@Data // @Data umfasst @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
public class HomeofficeRequestDto {
    @JsonFormat(pattern = "dd.MM.yyyy") // Passt das Datum von JS an (z.B. "19.06.2025")
    private LocalDate requestDate;
    private boolean isHalfDay;
}