package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Budget(Long id,
                     Double amount,
                     LocalDateTime startDate,
                     LocalDateTime endDate) {
}
