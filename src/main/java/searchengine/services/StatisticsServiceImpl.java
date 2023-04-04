package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    private TotalStatistics getTotal() {
        int sites = (int) siteRepository.count();
        int pages = (int) pageRepository.count();
        int lemmas = (int) lemmaRepository.count();
        return new TotalStatistics(sites, pages, lemmas, true);
    }

    private DetailedStatisticsItem getDetailed(Site site) {
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        Date statusTime = site.getStatusTime();
        String error = site.getLastError();
        int pages = pageRepository.countBySite(site);
        int lemmas = lemmaRepository.countBySite(site);
        return new DetailedStatisticsItem(url, name, status, statusTime, error, pages, lemmas);
    }

    private List<DetailedStatisticsItem> getDetailedList() {
        List<Site> siteList = siteRepository.findAll();
        List<DetailedStatisticsItem> result = new ArrayList<>();
        for (Site site : siteList) {
            DetailedStatisticsItem item = getDetailed(site);
            result.add(item);
        }
        return result;
    }

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = getTotal();
        List<DetailedStatisticsItem> list = getDetailedList();
        return new StatisticsResponse(true, new StatisticsData(total, list));
    }
}
