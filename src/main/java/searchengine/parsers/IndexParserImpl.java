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
import searchengine.utils.CleanHtmlCode;

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
    public void run(Site site) {
        Iterable<Page> pageList = pageRepository.findBySite(site);
        List<Lemma> lemmaList = lemmaRepository.findBySite(site);
        statisticsIndexList = new ArrayList<>();
        for (Page page : pageList) {
            if (page.getCode() < 400) {
                int pageId = page.getId();
                String content = page.getContent();
                HashMap<String, Integer> titleList = getTitleList(content);
                HashMap<String, Integer> bodyList = getBodyList(content);
                addStatisticsIndexList(lemmaList, titleList, bodyList, pageId);
            }
        }
    }

    private void addStatisticsIndexList(List<Lemma> lemmaList, HashMap<String, Integer> titleList, HashMap<String, Integer> bodyList, int pageId) {
        for (Lemma lemma : lemmaList) {
            Integer lemmaId = lemma.getId();
            String theExactLemma = lemma.getLemma();
            if (titleList.containsKey(theExactLemma) || bodyList.containsKey(theExactLemma)) {
                float wholeRank = getWholeRank(titleList, bodyList, theExactLemma);
                statisticsIndexList.add(new StatisticsIndex(pageId, lemmaId, wholeRank));
            }
        }
    }

    private float getWholeRank(HashMap<String, Integer> titleList, HashMap<String, Integer> bodyList, String theExactLemma) {
        float wholeRank = 0.0F;
        if (titleList.get(theExactLemma) != null) {
            Float titleRank = Float.valueOf(titleList.get(theExactLemma));
            wholeRank += titleRank;
        }
        if (bodyList.get(theExactLemma) != null) {
            float bodyRank = (float) (bodyList.get(theExactLemma) * 0.8);
            wholeRank += bodyRank;
        }
        return wholeRank;
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
