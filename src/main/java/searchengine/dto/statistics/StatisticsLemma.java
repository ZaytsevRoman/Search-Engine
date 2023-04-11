package searchengine.dto.statistics;

import lombok.Value;

@Value
public class StatisticsLemma {
    private String lemma;
    private int frequency;
}
