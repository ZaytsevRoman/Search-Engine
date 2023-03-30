package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.BadRequest;
import searchengine.dto.statistics.SearchResults;
import searchengine.dto.statistics.Response;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (indexingService.indexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new BadRequest(false,
                    "Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.indexingPage(url) == true) {
                return new ResponseEntity<>(new Response(true), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new BadRequest(false, "Данная страница находится за пределами сайтов, \n" +
                        "указанных в конфигурационном файле\n"),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String request,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (request.isEmpty()) {
            return new ResponseEntity<>(new BadRequest(false, "Задан пустой поисковый запрос"), HttpStatus.BAD_REQUEST);
        } else {
            List<StatisticsSearch> searchData;
            if (!site.isEmpty()) {
                if (siteRepository.findByUrl(site) == null) {
                    return new ResponseEntity<>(new BadRequest(false, "Указанная страница не найдена"),
                            HttpStatus.BAD_REQUEST);
                } else {
                    searchData = searchService.siteSearch(request, site, offset, limit);
                }
            } else {
                searchData = searchService.allSiteSearch(request, offset, limit);
            }
            return new ResponseEntity<>(new SearchResults(true, searchData.size(), searchData), HttpStatus.OK);
        }
    }
}
