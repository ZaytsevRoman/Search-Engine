package searchengine.parsers;

import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionConfiguration;
import searchengine.dto.statistics.StatisticsPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@AllArgsConstructor
public class UrlParser extends RecursiveTask<List<StatisticsPage>> {
    private final String url;
    private final List<String> urlList;
    private final List<StatisticsPage> statisticsPageList;
    private final ConnectionConfiguration connectionConfiguration;

    @Override
    protected List<StatisticsPage> compute() {
        try {
            Thread.sleep(500);
            Document document = getConnectionConfiguration();
            String html = document.outerHtml();
            Connection.Response response = document.connection().response();
            int statusCode = response.statusCode();
            StatisticsPage statisticsPage = new StatisticsPage(url, html, statusCode);
            statisticsPageList.add(statisticsPage);
            Elements links = document.select("body").select("a");
            List<UrlParser> taskList = new ArrayList<>();
            for (Element link : links) {
                String absUrl = link.attr("abs:href");
                if (isUrlCorrect(absUrl, link)) {
                    urlList.add(absUrl);
                    UrlParser task = new UrlParser(absUrl, urlList, statisticsPageList, connectionConfiguration);
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

    private boolean isUrlCorrect(String absUrl, Element link) {
        return (absUrl.startsWith(link.baseUri())
                && !absUrl.equals(link.baseUri())
                && !absUrl.contains("#")
                && !absUrl.matches("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)")
                && !urlList.contains(absUrl));
    }

    private Document getConnectionConfiguration() throws IOException {
        return Jsoup.connect(url)
                .userAgent(connectionConfiguration.getUserAgent())
                .referrer(connectionConfiguration.getReferrer())
                .timeout(500 + (int) (Math.random() * 4500))
                .get();
    }
}
