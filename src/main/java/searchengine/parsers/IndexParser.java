package searchengine.parsers;

import searchengine.dto.statistics.StatisticsIndex;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.utils.HtmlCodeCleaner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IndexParser {
    private static List<StatisticsIndex> statisticsIndexList;

    public static List<StatisticsIndex> getStatisticsIndexList() {
        return statisticsIndexList;
    }

    public static void statisticsIndexListParsing(Site site, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        Iterable<Page> pageList = pageRepository.findPageListBySite(site);
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySite(site);
        statisticsIndexList = new ArrayList<>();
        for (Page page : pageList) {
            if (page.getCode() < 400) {
                int pageId = page.getId();
                String content = page.getContent();
                HashMap<String, Integer> lemmaListFromTitle = getLemmaListFromTitle(content);
                HashMap<String, Integer> lemmaListFromBody = getLemmaListFromBody(content);
                addStatisticsIndexList(lemmaList, lemmaListFromTitle, lemmaListFromBody, pageId);
            }
        }
    }

    private static void addStatisticsIndexList(List<Lemma> lemmaList, HashMap<String, Integer> lemmaListFromTitle, HashMap<String, Integer> lemmaListFromBody, int pageId) {
        for (Lemma lemma : lemmaList) {
            Integer lemmaId = lemma.getId();
            String theExactLemma = lemma.getLemma();
            if (lemmaListFromTitle.containsKey(theExactLemma) || lemmaListFromBody.containsKey(theExactLemma)) {
                float rank = getRank(lemmaListFromTitle, lemmaListFromBody, theExactLemma);
                statisticsIndexList.add(new StatisticsIndex(pageId, lemmaId, rank));
            }
        }
    }

    private static float getRank(HashMap<String, Integer> lemmaListFromTitle, HashMap<String, Integer> lemmaListFromBody, String theExactLemma) {
        float rank = 0.0F;
        if (lemmaListFromTitle.get(theExactLemma) != null) {
            Float titleRank = Float.valueOf(lemmaListFromTitle.get(theExactLemma));
            rank += titleRank;
        }
        if (lemmaListFromBody.get(theExactLemma) != null) {
            float bodyRank = (float) (lemmaListFromBody.get(theExactLemma) * 0.8);
            rank += bodyRank;
        }
        return rank;
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
