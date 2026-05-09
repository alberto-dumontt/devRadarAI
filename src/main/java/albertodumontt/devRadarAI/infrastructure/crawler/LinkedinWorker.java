package albertodumontt.devRadarAI.infrastructure.crawler;

import albertodumontt.devRadarAI.model.JobResponse;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class LinkedinWorker {

    // geoId=106057199 = Brazil | f_TPR=r604800 = last 7 days
    private static final String SEARCH_URL =
            "https://www.linkedin.com/jobs/search?keywords=desenvolvedor&geoId=106057199&f_TPR=r604800";

    private static final String BRAVE_PATH =
            "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser";

    public List<JobResponse> crawl() {
        log.info("LinkedinWorker: starting crawl");
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")))) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setExecutablePath(Path.of(BRAVE_PATH))
            );

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
                    .setLocale("pt-BR")
                    .setExtraHTTPHeaders(Map.of("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8"))
            );

            Page page = context.newPage();

            // Step 1: collect basic data from search results
            page.navigate(SEARCH_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForSelector("ul.jobs-search__results-list", new Page.WaitForSelectorOptions().setTimeout(15_000));

            List<JobResponse> jobs = page.querySelectorAll("ul.jobs-search__results-list > li").stream()
                    .map(this::parseCard)
                    .filter(Objects::nonNull)
                    .toList();

            log.info("LinkedinWorker: {} jobs found, fetching details...", jobs.size());

            // Step 2: enrich each job with detail page data
            List<JobResponse> result = jobs.stream()
                    .map(job -> withDetails(page, job))
                    .toList();

            log.info("LinkedinWorker: crawl complete");
            return result;

        } catch (Exception e) {
            log.error("LinkedinWorker: crawl failed — {}", e.getMessage());
            return List.of();
        }
    }

    private JobResponse parseCard(ElementHandle card) {
        try {
            ElementHandle titleEl    = card.querySelector("h3.base-search-card__title");
            ElementHandle companyEl  = card.querySelector("h4.base-search-card__subtitle");
            ElementHandle locationEl = card.querySelector("span.job-search-card__location");
            ElementHandle linkEl     = card.querySelector("a.base-card__full-link");
            ElementHandle timeEl     = card.querySelector("time.job-search-card__listdate");

            String title    = titleEl    != null ? titleEl.innerText().trim()         : "";
            String company  = companyEl  != null ? companyEl.innerText().trim()       : "";
            String location = locationEl != null ? locationEl.innerText().trim()      : "";
            String url      = linkEl     != null ? linkEl.getAttribute("href").trim() : "";
            String datetime = timeEl     != null ? timeEl.getAttribute("datetime")    : "";

            if (title.isBlank() || url.isBlank()) return null;

            LocalDate publishedAt = (datetime != null && !datetime.isBlank())
                    ? LocalDate.parse(datetime) : null;

            return new JobResponse(title, company, location, null, null, null, false, url, publishedAt);

        } catch (Exception e) {
            log.warn("LinkedinWorker: failed to parse card — {}", e.getMessage());
            return null;
        }
    }

    private JobResponse withDetails(Page page, JobResponse job) {
        try {
            page.navigate(job.url(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(2_000);

            // Workplace type: Remote / Hybrid / On-site
            ElementHandle workplaceEl = page.querySelector(
                    "span.job-details-jobs-unified-top-card__workplace-type"
            );
            String workplaceType = workplaceEl != null ? workplaceEl.innerText().trim() : null;

            // Criteria list: seniority level, employment type, etc.
            Map<String, String> criteria = parseCriteria(page);
            String seniorityLevel  = criteria.get("seniority level");
            String employmentType  = criteria.get("employment type");

            // Fallback pt-BR keys
            if (seniorityLevel == null)  seniorityLevel  = criteria.get("nível de experiência");
            if (employmentType == null)  employmentType  = criteria.get("tipo de emprego");

            boolean remote = workplaceType != null &&
                    (workplaceType.toLowerCase().contains("remote") ||
                     workplaceType.toLowerCase().contains("remoto"));

            log.debug("LinkedinWorker: details fetched for '{}'", job.title());
            return new JobResponse(
                    job.title(), job.company(), job.location(),
                    workplaceType, employmentType, seniorityLevel,
                    remote, job.url(), job.publishedAt()
            );

        } catch (Exception e) {
            log.warn("LinkedinWorker: failed to fetch details for '{}' — {}", job.title(), e.getMessage());
            return job;
        }
    }

    private Map<String, String> parseCriteria(Page page) {
        Map<String, String> criteria = new HashMap<>();
        try {
            List<ElementHandle> items = page.querySelectorAll("ul.description__job-criteria-list > li");
            for (ElementHandle item : items) {
                ElementHandle header = item.querySelector("h3.description__job-criteria-subheader");
                ElementHandle value  = item.querySelector("span.description__job-criteria-text");
                if (header != null && value != null) {
                    criteria.put(header.innerText().trim().toLowerCase(), value.innerText().trim());
                }
            }
        } catch (Exception ignored) {}
        return criteria;
    }
}
