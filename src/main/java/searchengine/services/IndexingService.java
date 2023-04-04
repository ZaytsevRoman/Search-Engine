package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface IndexingService {
    ResponseEntity<Object> indexing();
    ResponseEntity<Object> stopIndexing();
    ResponseEntity<Object> indexingPage(String url);
}
