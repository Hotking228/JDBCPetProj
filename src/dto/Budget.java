package dto;

import java.math.BigDecimal;

public record Budget(Long id,
                     Long category_id,
                     BigDecimal amount,
                     int month,
                     int year) {
}
