package searchengine.dto.statistics;

import lombok.Value;

@Value
public class StatisticsIndex {
    private Integer pageId;
    private Integer lemmaId;
    private Float rank;
}
