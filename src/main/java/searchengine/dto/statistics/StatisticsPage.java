package searchengine.dto.statistics;

import lombok.Value;

@Value
public class StatisticsPage {
    private String url;
    private String content;
    private int code;
}
