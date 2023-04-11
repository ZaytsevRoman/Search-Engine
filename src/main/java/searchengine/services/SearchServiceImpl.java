package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.SearchResults;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.HtmlCodeCleaner;

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
    public ResponseEntity<Object> search(String text, String url, int offset, int limit) {
        ResponseEntity<Object> result;
        if (text.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Задан пустой поисковый запрос"), HttpStatus.BAD_REQUEST);
        } else {
            result = getSearchData(text, url, offset, limit);
        }
        return result;
    }

    private ResponseEntity<Object> getSearchData(String text, String url, int offset, int limit) {
        List<StatisticsSearch> searchData;
        if (!url.isEmpty()) {
            if (siteRepository.findSiteByUrl(url) == null) {
                return new ResponseEntity<>(new BadRequest(false, "Указанная страница не найдена"),
                        HttpStatus.BAD_REQUEST);
            } else {
                searchData = siteSearch(text, url, offset, limit);
            }
        } else {
            searchData = allSiteSearch(text, offset, limit);
        }
        return new ResponseEntity<>(new SearchResults(true, searchData.size(), searchData), HttpStatus.OK);
    }

    private List<StatisticsSearch> allSiteSearch(String text, int offset, int limit) {
        log.info("Поиск по всем сайтам: " + text);
        List<Site> siteList = siteRepository.findAll();
        List<String> lemmasFromSearchText = getLemmasFromSearchText(text);
        List<Lemma> foundLemmaListAllSite = new ArrayList<>();
        List<Page> pageListWithLemmas = new ArrayList<>();
        List<StatisticsSearch> result = new ArrayList<>();
        for (int i = 0; i < siteList.size(); i++) {
            List<Lemma> foundLemmaList = getLemmaListFromSite(lemmasFromSearchText, siteList.get(i));
            foundLemmaListAllSite.addAll(foundLemmaList);
            pageListWithLemmas.addAll(getPageListWithLemmas(foundLemmaList, offset, limit));
        }
        List<StatisticsSearch> dataList = getStatisticSearchList(foundLemmaListAllSite, pageListWithLemmas, lemmasFromSearchText);
        if (dataList.size() > limit) {
            for (int i = 0; i < limit; i++) {
                result.add(dataList.get(i));
            }
            return result;
        }
        log.info("Поиск завершен");
        return dataList;
    }

    private List<StatisticsSearch> siteSearch(String text, String url, int offset, int limit) {
        log.info("Поиск: " + text + ", на сайте: " + url);
        Site site = siteRepository.findSiteByUrl(url);
        List<String> lemmasFromSearchText = getLemmasFromSearchText(text);
        List<Lemma> foundLemmaList = getLemmaListFromSite(lemmasFromSearchText, site);
        List<Page> result = getPageListWithLemmas(foundLemmaList, offset, limit);
        List<StatisticsSearch> dataList = getStatisticSearchList(foundLemmaList, result, lemmasFromSearchText);
        log.info("Поиск завершен");
        return dataList;
    }

    private List<StatisticsSearch> getStatisticSearchList(List<Lemma> foundLemmaList, List<Page> pageList, List<String> lemmasFromSearchText) {
        indexRepository.flush();
        List<Index> indexList = indexRepository.findIndexListByPagesAndLemmas(foundLemmaList, pageList);
        Hashtable<Page, Float> pageListWithAbsRelevance = getPageListWithAbsRelevance(pageList, indexList);
        List<StatisticsSearch> dataList = getSearchData(pageListWithAbsRelevance, lemmasFromSearchText);
        dataList.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        return dataList;
    }

    private List<String> getLemmasFromSearchText(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String word : words) {
            lemmaList.addAll(morphology.getLemmaList(word));
        }
        return lemmaList;
    }

    private List<Lemma> getLemmaListFromSite(List<String> lemmasFromSearchText, Site site) {
        lemmaRepository.flush();
        List<Lemma> lemmaList = new ArrayList<>();
        List<Lemma> result = lemmaRepository.findLemmaListBySiteAndLemmas(lemmasFromSearchText, site);
        if (result.size() >= lemmasFromSearchText.size()) {
            lemmaList = result;
            lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        }
        return lemmaList;
    }

    private List<Page> getPageListWithLemmas(List<Lemma> foundLemmaList, int offset, int limit) {
        List<Page> result = new ArrayList<>();
        List<Page> foundPageList = new ArrayList<>();
        if (foundLemmaList.size() == 1) {
            foundPageList = getPageListWithOneLemma(foundLemmaList);
        }
        if (foundLemmaList.size() > 1) {
            foundPageList = getPageListWithSomeLemmas(foundLemmaList);
        }
        if (offset > foundPageList.size()) {
            return new ArrayList<>();
        }
        if (foundPageList.size() > limit) {
            for (int i = 0; i < limit; i++) {
                result.add(foundPageList.get(i));
            }
            return result;
        } else return foundPageList;
    }

    private List<Page> getPageListWithOneLemma(List<Lemma> foundLemmaList) {
        pageRepository.flush();
        List<Page> foundPageList = pageRepository.findPageListByLemma(foundLemmaList.get(0));
        return foundPageList;
    }

    private List<Page> getPageListWithSomeLemmas(List<Lemma> foundLemmaList) {
        List<Page> foundPageList = new ArrayList<>();
        pageRepository.flush();
        List<Page> pageList = pageRepository.findPageListByLemma(foundLemmaList.get(0));
        for (int i = 1; i < foundLemmaList.size(); i++) {
            foundPageList.addAll(getPageListByLemmas(foundLemmaList, i, pageList));
        }
        return foundPageList;
    }

    private List<Page> getPageListByLemmas(List<Lemma> foundLemmaList, int i, List<Page> pageList) {
        List<Page> foundPageList = new ArrayList<>();
        pageList.removeIf(page -> indexRepository.findIndexListByPageAndLemma(foundLemmaList.get(i), page).isEmpty());
        if (i == (foundLemmaList.size() - 1)) {
            foundPageList = pageList;
        }
        return foundPageList;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<Page, Float> pageList, List<String> lemmasFromSearchText) {
        List<StatisticsSearch> result = new ArrayList<>();
        for (Page page : pageList.keySet()) {
            Site pageSite = page.getSite();
            String uri = pageSite.getUrl() + page.getPath();
            String content = page.getContent();
            String address = pageSite.getUrl();
            String siteName = pageSite.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = HtmlCodeCleaner.getClearHtmlCode(content, "title");
            String body = HtmlCodeCleaner.getClearHtmlCode(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), lemmasFromSearchText);

            result.add(new StatisticsSearch(address, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, List<String> lemmasFromSearchText) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmasFromSearchText) {
            lemmaIndexList.addAll(morphology.getLemmaIndexList(content, lemma));
        }
        Collections.sort(lemmaIndexList);
        List<String> resultList = getWordsFromContent(content, lemmaIndexList);
        for (int i = 0; i < resultList.size(); i++) {
            result.append(resultList.get(i)).append("... ");
            if (i > 5) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndexList) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndexList.size(); i++) {
            int start = lemmaIndexList.get(i);
            int end = content.indexOf(" ", start);
            int nextPoint = i + 1;
            while (nextPoint < lemmaIndexList.size() && lemmaIndexList.get(nextPoint) - end > 0 && lemmaIndexList.get(nextPoint) - end < 5) {
                end = content.indexOf(" ", lemmaIndexList.get(nextPoint));
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

    private Hashtable<Page, Float> getPageListWithAbsRelevance(List<Page> pageList, List<Index> indexList) {
        HashMap<Page, Float> pageWithRelevance = new HashMap<>();
        for (Page page : pageList) {
            float relevance = getRelevance(indexList, page);
            pageWithRelevance.put(page, relevance);
        }
        HashMap<Page, Float> pageWithAbsRelevance = new HashMap<>();
        for (Page page : pageWithRelevance.keySet()) {
            float absRelevance = pageWithRelevance.get(page) / Collections.max(pageWithRelevance.values());
            pageWithAbsRelevance.put(page, absRelevance);
        }
        return pageWithAbsRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }

    private float getRelevance(List<Index> indexList, Page page) {
        float relevance = 0;
        for (Index index : indexList) {
            if (index.getPage() == page) {
                relevance += index.getRank();
            }
        }
        return relevance;
    }
}
