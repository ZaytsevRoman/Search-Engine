package searchengine.parsers;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.statistics.StatisticsPage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class UrlParser extends RecursiveTask<List<StatisticsPage>> {
    private final String url;
    private final List<String> urlList;
    private final List<StatisticsPage> statisticsPageList;

    public UrlParser(String url, List<StatisticsPage> statisticsPageList, List<String> urlList) {
        this.url = url;
        this.statisticsPageList = statisticsPageList;
        this.urlList = urlList;
    }

    @Override
    protected List<StatisticsPage> compute() {
        try {
            Thread.sleep(150);
            Document document =  Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(500 + (int) (Math.random() * 4500))
                    .get();
            String html = document.outerHtml();
            Connection.Response response = document.connection().response();
            int statusCode = response.statusCode();
            StatisticsPage statisticsPage = new StatisticsPage(url, html, statusCode);
            statisticsPageList.add(statisticsPage);
            Elements links = document.select("body").select("a");
            List<UrlParser> taskList = new ArrayList<>();
            for (Element link : links) {
                String absUrl = link.attr("abs:href");
                if (absUrl.startsWith(link.baseUri())
                        && !absUrl.equals(link.baseUri())
                        && !absUrl.contains("#")
                        && !absUrl.matches("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)")
                        && !urlList.contains(absUrl)) {
                    urlList.add(absUrl);
                    UrlParser task = new UrlParser(absUrl, statisticsPageList, urlList);
                    task.fork();
                    taskList.add(task);
                }
            }
            taskList.forEach(ForkJoinTask::join);
        } catch (Exception e) {
            StatisticsPage statisticsPage = new StatisticsPage(url, "", 500);
            statisticsPageList.add(statisticsPage);
        }
        return statisticsPageList;
    }
}
