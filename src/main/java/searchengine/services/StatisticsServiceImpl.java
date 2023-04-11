package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatistics;
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

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatistics> list = getDetailedStatisticsList();
        return new StatisticsResponse(true, new StatisticsData(total, list));
    }

    private TotalStatistics getTotalStatistics() {
        int sites = (int) siteRepository.count();
        int pages = (int) pageRepository.count();
        int lemmas = (int) lemmaRepository.count();
        return new TotalStatistics(sites, pages, lemmas, true);
    }

    private DetailedStatistics getDetailedStatistics(Site site) {
        String url = site.getUrl();
        String name = site.getName();
        Status status = site.getStatus();
        Date statusTime = site.getStatusTime();
        String error = site.getLastError();
        int pages = pageRepository.countBySite(site);
        int lemmas = lemmaRepository.countBySite(site);
        return new DetailedStatistics(url, name, status, statusTime, error, pages, lemmas);
    }

    private List<DetailedStatistics> getDetailedStatisticsList() {
        List<Site> siteList = siteRepository.findAll();
        List<DetailedStatistics> result = new ArrayList<>();
        for (Site site : siteList) {
            DetailedStatistics detailedSiteStatistics = getDetailedStatistics(site);
            result.add(detailedSiteStatistics);
        }
        return result;
    }
}
