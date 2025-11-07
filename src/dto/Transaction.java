package dto;

import java.time.LocalDateTime;

public record Transaction(Long id,
                          Double amount,
                          Long category_id,
                          LocalDateTime date,
                          String description) {
}
