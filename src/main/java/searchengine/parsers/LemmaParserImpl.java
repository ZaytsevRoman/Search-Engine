package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.MorphologyService;
import searchengine.repository.PageRepository;
import searchengine.utils.HtmlCodeCleaner;

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
    public void statisticsLemmaListParsing(Site site) {
        statisticsLemmaList = new CopyOnWriteArrayList<>();
        Iterable<Page> pageList = pageRepository.findPageListBySite(site);
        TreeMap<String, Integer> lemmaList = new TreeMap<>();
        for (Page page : pageList) {
            String content = page.getContent();
            HashMap<String, Integer> lemmaListFromTitle = getLemmaListFromTitle(content);
            HashMap<String, Integer> lemmaListFromBody = getLemmaListFromBody(content);
            Set<String> allTheWords = new HashSet<>();
            allTheWords.addAll(lemmaListFromTitle.keySet());
            allTheWords.addAll(lemmaListFromBody.keySet());
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

    private HashMap<String, Integer> getLemmaListFromTitle(String content) {
        String title = HtmlCodeCleaner.getClearHtmlCode(content, "title");
        HashMap<String, Integer> lemmaListFromTitle = morphology.getLemmaListWithCount(title);
        return lemmaListFromTitle;
    }

    private HashMap<String, Integer> getLemmaListFromBody(String content) {
        String body = HtmlCodeCleaner.getClearHtmlCode(content, "body");
        HashMap<String, Integer> lemmaListFromBody = morphology.getLemmaListWithCount(body);
        return lemmaListFromBody;
    }
}
