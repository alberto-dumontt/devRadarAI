package albertodumontt.devRadarAI.infrastructure.crawler;

import albertodumontt.devRadarAI.model.JobResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class LinkedinWorker {

    // geoId=106057199 = Brazil | f_TPR=r604800 = last 7 days
    private static final String SEARCH_URL =
            "https://www.linkedin.com/jobs/search?keywords=desenvolvedor&geoId=106057199&f_TPR=r604800";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36";

    public List<JobResponse> crawl() {
        log.info("LinkedinWorker: starting crawl");
        try {
            Document doc = Jsoup.connect(SEARCH_URL)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "pt-BR,pt;q=0.9")
                    .timeout(15_000)
                    .get();

            List<JobResponse> jobs = doc.select("ul.jobs-search__results-list > li").stream()
                    .map(this::parseCard)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("LinkedinWorker: {} jobs found", jobs.size());
            return jobs;

        } catch (IOException e) {
            log.error("LinkedinWorker: crawl failed — {}", e.getMessage());
            return List.of();
        }
    }

    private JobResponse parseCard(Element card) {
        try {
            String title      = card.select("h3.base-search-card__title").text().trim();
            String company    = card.select("h4.base-search-card__subtitle").text().trim();
            String location   = card.select("span.job-search-card__location").text().trim();
            String url        = card.select("a.base-card__full-link").attr("href").trim();
            String datetime   = card.select("time.job-search-card__listdate").attr("datetime").trim();
            LocalDate publishedAt = datetime.isBlank() ? null : LocalDate.parse(datetime);

            if (title.isBlank() || url.isBlank()) return null;

            boolean remote = location.toLowerCase().contains("remote")
                    || location.toLowerCase().contains("remoto");

            return new JobResponse(title, company, location, remote, url, publishedAt);

        } catch (Exception e) {
            log.warn("LinkedinWorker: failed to parse card — {}", e.getMessage());
            return null;
        }
    }
}
