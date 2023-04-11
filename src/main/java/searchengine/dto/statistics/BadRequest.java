package searchengine.dto.statistics;

import lombok.Value;

@Value
public class BadRequest {
    private boolean result;
    private String error;
}
