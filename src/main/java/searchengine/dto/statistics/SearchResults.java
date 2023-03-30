package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResults {
    private boolean result;
    private int count;
    private List<StatisticsSearch> data;
}
