package searchengine.dto.statistics;

import lombok.Value;

@Value
public class TotalStatistics {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
