package searchengine.parsers;

import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.utils.HtmlCodeCleaner;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LemmaParser {
    private static List<StatisticsLemma> statisticsLemmaList;

    public static List<StatisticsLemma> getStatisticsLemmaList() {
        return statisticsLemmaList;
    }

    public static void statisticsLemmaListParsing(Site site, PageRepository pageRepository) {
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

    private static void addStatisticsLemmaList(TreeMap<String, Integer> lemmaList) {
        for (String lemma : lemmaList.keySet()) {
            Integer frequency = lemmaList.get(lemma);
            statisticsLemmaList.add(new StatisticsLemma(lemma, frequency));
        }
    }

    private static HashMap<String, Integer> getLemmaListFromTitle(String content) {
        String title = HtmlCodeCleaner.getClearHtmlCode(content, "title");
        HashMap<String, Integer> lemmaListFromTitle = Morphology.getLemmaListWithCount(title);
        return lemmaListFromTitle;
    }

    private static HashMap<String, Integer> getLemmaListFromBody(String content) {
        String body = HtmlCodeCleaner.getClearHtmlCode(content, "body");
        HashMap<String, Integer> lemmaListFromBody = Morphology.getLemmaListWithCount(body);
        return lemmaListFromBody;
    }
}
