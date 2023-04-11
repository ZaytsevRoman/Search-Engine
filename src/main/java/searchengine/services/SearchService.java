package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface SearchService {
    ResponseEntity<Object> search(String text, String url, int offset, int limit);
}
