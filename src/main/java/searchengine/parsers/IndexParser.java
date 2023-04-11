package searchengine.parsers;

import searchengine.dto.statistics.StatisticsIndex;
import searchengine.model.Site;

import java.util.List;

public interface IndexParser {
    void statisticsIndexListParsing(Site site);
    List<StatisticsIndex> getStatisticsIndexList();
}
