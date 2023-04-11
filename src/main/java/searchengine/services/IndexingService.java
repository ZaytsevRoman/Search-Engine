package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface IndexingService {
    ResponseEntity<Object> startIndexing();
    ResponseEntity<Object> stopIndexing();
    ResponseEntity<Object> onePageIndexing(String url);
}
