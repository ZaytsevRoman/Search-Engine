package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.statistics.StatisticsSearch;

import java.util.List;

public interface SearchService {
    ResponseEntity<Object> search(String text, String url, int offset, int limit);
}
