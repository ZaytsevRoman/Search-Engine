package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.CleanHtmlCode;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final MorphologyService morphology;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Override
    public List<StatisticsSearch> allSiteSearch(String text, int offset, int limit) {
        log.info("Поиск по всем сайтам: " + text);
        List<Site> siteList = siteRepository.findAll();
        List<String> wordsList = getWordsFromSearchText(text);
        List<Lemma> foundLemmaList = new ArrayList<>();
        for (Site site : siteList) {
            foundLemmaList.addAll(getLemmaListFromSite(wordsList, site));
        }
        List<StatisticsSearch> result = new ArrayList<>(getStatisticSearchList(foundLemmaList, wordsList, offset, limit));
        result.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        log.info("Поиск завершен");
        return result;
    }

    @Override
    public List<StatisticsSearch> siteSearch(String text, String url, int offset, int limit) {
        log.info("Поиск: " + text + ", на сайте: " + url);
        Site site = siteRepository.findByUrl(url);
        List<String> wordsList = getWordsFromSearchText(text);
        List<Lemma> foundLemmaList = getLemmaListFromSite(wordsList, site);
        List<StatisticsSearch> result = new ArrayList<>(getStatisticSearchList(foundLemmaList, wordsList, offset, limit));
        result.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        log.info("Поиск завершен");
        return result;
    }

    private List<String> getWordsFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> wordsList = new ArrayList<>();
        for (String word : words) {
            wordsList.addAll(morphology.getLemma(word));
        }
        return wordsList;
    }

    private List<Lemma> getLemmaListFromSite(List<String> words, Site site) {
        lemmaRepository.flush();
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySite(words, site);
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private List<StatisticsSearch> getStatisticSearchList(List<Lemma> lemmaList, List<String> wordsList, int offset, int limit) {
        List<StatisticsSearch> result = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= wordsList.size()) {
            List<Page> foundPageList = pageRepository.findByLemmaList(lemmaList);
            indexRepository.flush();
            List<Index> foundIndexList = indexRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Hashtable<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<StatisticsSearch> dataList = getSearchData(sortedPageByAbsRelevance, wordsList);

            if (offset > dataList.size()) {
                return new ArrayList<>();
            }

            if (dataList.size() > limit) {
                for (int i = offset; i < limit; i++) {
                    result.add(dataList.get(i));
                }
                return result;
            } else return dataList;
        } else return result;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<Page, Float> pageList, List<String> wordsList) {
        List<StatisticsSearch> result = new ArrayList<>();
        for (Page page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteId();
            String address = pageSite.getUrl();
            String siteName = pageSite.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = CleanHtmlCode.clear(content, "title");
            String body = CleanHtmlCode.clear(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), wordsList);

            result.add(new StatisticsSearch(address, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, List<String> wordsList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String words : wordsList) {
            lemmaIndex.addAll(morphology.findLemmaIndexInText(content, words));
        }
        Collections.sort(lemmaIndex);
        List<String> resultList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < resultList.size(); i++) {
            result.append(resultList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndex.size() && lemmaIndex.get(nextPoint) - end > 0 && lemmaIndex.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextPoint));
                nextPoint += 1;
            }
            i = nextPoint - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start) != -1) {
            prevPoint = content.lastIndexOf(" ", start);
        } else prevPoint = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastPoint = content.indexOf(" ", end + 30);
        } else lastPoint = content.indexOf(" ", end);
        String text = content.substring(prevPoint, lastPoint);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private Hashtable<Page, Float> getPageAbsRelevance(List<Page> pageList, List<Index> indexList) {
        HashMap<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pageList) {
            float relevant = 0;
            for (Index index : indexList) {
                if (index.getPage() == page) {
                    relevant += index.getRank();
                }
            }
            pageWithRelevance.put(page, relevant);
        }
        HashMap<Page, Float> pageWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float absRelevant = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, absRelevant);
        }
        return pageWithAbsRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }
}
