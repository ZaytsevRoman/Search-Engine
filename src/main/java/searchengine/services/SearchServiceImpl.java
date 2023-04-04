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
            if (siteRepository.findByUrl(url) == null) {
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
        List<String> wordsList = getWordsFromSearchText(text);
        List<Lemma> foundLemmaListAllSite = new ArrayList<>();
        List<Page> result = new ArrayList<>();
        for (int i = 0; i < siteList.size(); i++) {
            List<Lemma> foundLemmaList = getLemmaListFromSite(wordsList, siteList.get(i));
            foundLemmaListAllSite.addAll(getLemmaListFromSite(wordsList, siteList.get(i)));
            result.addAll(getPageListWithLemmas(foundLemmaList, offset, limit));
        }
        List<StatisticsSearch> dataList = getListStatisticSearch(foundLemmaListAllSite, result, wordsList);
        log.info("Поиск завершен");
        return dataList;
    }

    private List<StatisticsSearch> siteSearch(String text, String url, int offset, int limit) {
        log.info("Поиск: " + text + ", на сайте: " + url);
        Site site = siteRepository.findByUrl(url);
        List<String> wordsList = getWordsFromSearchText(text);
        List<Lemma> foundLemmaList = getLemmaListFromSite(wordsList, site);
        List<Page> result = new ArrayList<>(getPageListWithLemmas(foundLemmaList, offset, limit));
        List<StatisticsSearch> dataList = getListStatisticSearch(foundLemmaList, result, wordsList);
        log.info("Поиск завершен");
        return dataList;
    }

    private List<StatisticsSearch> getListStatisticSearch(List<Lemma> lemmaList, List<Page> pageList, List<String> wordsList) {
        indexRepository.flush();
        List<Index> foundIndexList = indexRepository.findByPagesAndLemmas(lemmaList, pageList);
        Hashtable<Page, Float> sortedPageByAbsRelevance = getPageAbsRelevance(pageList, foundIndexList);
        List<StatisticsSearch> dataList = getSearchData(sortedPageByAbsRelevance, wordsList);
        dataList.sort((o1, o2) -> Float.compare(o2.getRelevance(), o1.getRelevance()));
        return dataList;
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
        List<Lemma> lemmaList = new ArrayList<>();
        List<Lemma> result = lemmaRepository.findLemmaListBySite(words, site);
        if (result.size() >= words.size()) {
            lemmaList = result;
            lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        }
        return lemmaList;
    }

    private List<Page> getPageListWithLemmas(List<Lemma> lemmaList, int offset, int limit) {
        List<Page> result = new ArrayList<>();
        List<Page> resultFoundPageList = new ArrayList<>();
        if (lemmaList.size() == 1) {
            resultFoundPageList = getPageListWithOneLemma(lemmaList);
        }
        if (lemmaList.size() > 1) {
            resultFoundPageList = getPageListWithSomeLemmas(lemmaList);
        }
        if (offset > resultFoundPageList.size()) {
            return new ArrayList<>();
        }
        if (resultFoundPageList.size() > limit) {
            for (int i = offset; i < limit; i++) {
                result.add(resultFoundPageList.get(i));
            }
            return result;
        } else return resultFoundPageList;
    }

    private List<Page> getPageListWithOneLemma(List<Lemma> lemmaList) {
        pageRepository.flush();
        List<Page> resultFoundPageList = pageRepository.findByLemma(lemmaList.get(0));
        return resultFoundPageList;
    }

    private List<Page> getPageListWithSomeLemmas(List<Lemma> lemmaList) {
        List<Page> resultFoundPageList = new ArrayList<>();
        for (int i = 1; i < lemmaList.size(); i++) {
            resultFoundPageList.addAll(getPageListByLemma(lemmaList, i));
        }
        return resultFoundPageList;
    }

    private List<Page> getPageListByLemma(List<Lemma> lemmaList, int i) {
        List<Page> resultFoundPageList = new ArrayList<>();
        pageRepository.flush();
        List<Page> pageList = pageRepository.findByLemma(lemmaList.get(i - 1));
        for (Page page : pageList) {
            if (!indexRepository.findByPageAndLemma(lemmaList.get(i), page).isEmpty() && i < (lemmaList.size() - 1)) {
                continue;
            } else if (i == (lemmaList.size() - 1) && !indexRepository.findByPageAndLemma(lemmaList.get(i), page).isEmpty()) {
                resultFoundPageList.add(page);
            }
        }
        return resultFoundPageList;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<Page, Float> pageList, List<String> wordsList) {
        List<StatisticsSearch> result = new ArrayList<>();
        for (Page page : pageList.keySet()) {
            Site pageSite = page.getSite();
            String uri = pageSite.getUrl() + page.getPath();
            String content = page.getContent();
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
            float relevant = getRelevant(indexList, page);
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

    private float getRelevant(List<Index> indexList, Page page) {
        float relevant = 0;
        for (Index index : indexList) {
            if (index.getPage() == page) {
                relevant += index.getRank();
            }
        }
        return relevant;
    }
}
