package searchengine.services;

import java.util.HashMap;
import java.util.List;

public interface MorphologyService {
    HashMap<String, Integer> getLemmaListWithCount(String content);
    List<String> getLemmaList(String word);
    List<Integer> getLemmaIndexList(String content, String lemma);
}
