package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsIndex;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.MorphologyService;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.utils.HtmlCodeCleaner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IndexParserImpl implements IndexParser {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final MorphologyService morphology;
    private List<StatisticsIndex> statisticsIndexList;

    @Override
    public List<StatisticsIndex> getStatisticsIndexList() {
        return statisticsIndexList;
    }

    @Override
    public void statisticsIndexListParsing(Site site) {
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

    private void addStatisticsIndexList(List<Lemma> lemmaList, HashMap<String, Integer> lemmaListFromTitle, HashMap<String, Integer> lemmaListFromBody, int pageId) {
        for (Lemma lemma : lemmaList) {
            Integer lemmaId = lemma.getId();
            String theExactLemma = lemma.getLemma();
            if (lemmaListFromTitle.containsKey(theExactLemma) || lemmaListFromBody.containsKey(theExactLemma)) {
                float rank = getRank(lemmaListFromTitle, lemmaListFromBody, theExactLemma);
                statisticsIndexList.add(new StatisticsIndex(pageId, lemmaId, rank));
            }
        }
    }

    private float getRank(HashMap<String, Integer> lemmaListFromTitle, HashMap<String, Integer> lemmaListFromBody, String theExactLemma) {
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
