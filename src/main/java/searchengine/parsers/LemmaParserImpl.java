package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.MorphologyService;
import searchengine.repository.PageRepository;
import searchengine.utils.CleanHtmlCode;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class LemmaParserImpl implements LemmaParser {
    private final PageRepository pageRepository;
    private final MorphologyService morphology;
    private List<StatisticsLemma> statisticsLemmaList;

    public List<StatisticsLemma> getStatisticsLemmaList() {
        return statisticsLemmaList;
    }

    @Override
    public void run(Site site) {
        statisticsLemmaList = new CopyOnWriteArrayList<>();
        Iterable<Page> pageList = pageRepository.findAll();
        TreeMap<String, Integer> lemmaList = new TreeMap<>();
        for (Page page : pageList) {
            String content = page.getContent();
            HashMap<String, Integer> titleList = getTitleList(content);
            HashMap<String, Integer> bodyList = getBodyList(content);
            Set<String> allTheWords = new HashSet<>();
            allTheWords.addAll(titleList.keySet());
            allTheWords.addAll(bodyList.keySet());
            for (String word : allTheWords) {
                int frequency = lemmaList.getOrDefault(word, 0) + 1;
                lemmaList.put(word, frequency);
            }
        }
        addStatisticsLemmaList(lemmaList);
    }

    private void addStatisticsLemmaList(TreeMap<String, Integer> lemmaList) {
        for (String lemma : lemmaList.keySet()) {
            Integer frequency = lemmaList.get(lemma);
            statisticsLemmaList.add(new StatisticsLemma(lemma, frequency));
        }
    }

    private HashMap<String, Integer> getTitleList(String content) {
        String title = CleanHtmlCode.clear(content, "title");
        HashMap<String, Integer> titleList = morphology.getLemmaList(title);
        return titleList;
    }

    private HashMap<String, Integer> getBodyList(String content) {
        String body = CleanHtmlCode.clear(content, "body");
        HashMap<String, Integer> bodyList = morphology.getLemmaList(body);
        return bodyList;
    }
}
