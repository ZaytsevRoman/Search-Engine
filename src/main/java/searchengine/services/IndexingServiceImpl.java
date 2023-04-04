package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.Response;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.parsers.IndexParser;
import searchengine.parsers.LemmaParser;
import searchengine.parsers.SiteIndexing;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final int processorCoreCount = Runtime.getRuntime().availableProcessors();
    private ExecutorService executorService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final SitesList sitesList;


    @Override
    public ResponseEntity<Object> indexing() {
        if (isIndexingStarted()) {
            return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);
        } else {
            List<searchengine.config.Site> siteList = sitesList.getSites();
            executorService = Executors.newFixedThreadPool(processorCoreCount);
            for (searchengine.config.Site site : siteList) {
                String url = site.getUrl();
                Site newSite = new Site();
                newSite.setName(site.getName());
                executorService.submit(new SiteIndexing(pageRepository, siteRepository, lemmaRepository, indexRepository, lemmaParser, indexParser, url, sitesList));
            }
            executorService.shutdown();
        }
        return new ResponseEntity<>(new Response(true), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> stopIndexing() {
        if (isIndexingStarted()) {
            executorService.shutdownNow();
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false,
                    "Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isIndexingStarted() {
        siteRepository.flush();
        Iterable<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseEntity<Object> indexingPage(String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else if (urlCheck(url)) {
            executorService = Executors.newFixedThreadPool(processorCoreCount);
            executorService.submit(new SiteIndexing(pageRepository, siteRepository, lemmaRepository, indexRepository, lemmaParser, indexParser, url, sitesList));
            executorService.shutdown();
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле\n"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean urlCheck(String url) {
        List<searchengine.config.Site> urlList = sitesList.getSites();
        for (searchengine.config.Site site : urlList) {
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }
}
