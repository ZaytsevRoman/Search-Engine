package searchengine.services;

public interface IndexingService {
    boolean indexing();
    boolean stopIndexing();
    boolean indexingPage(String url);
}
