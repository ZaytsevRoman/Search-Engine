package searchengine.dto.statistics;

import lombok.Value;
import searchengine.model.Status;

import java.util.Date;

@Value
public class DetailedStatistics {
    private String url;
    private String name;
    private Status status;
    private Date statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
