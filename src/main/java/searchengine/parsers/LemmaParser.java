package searchengine.parsers;

import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.Site;

import java.util.List;

public interface LemmaParser {
    void run(Site site);
    List<StatisticsLemma> getStatisticsLemmaList();
}
