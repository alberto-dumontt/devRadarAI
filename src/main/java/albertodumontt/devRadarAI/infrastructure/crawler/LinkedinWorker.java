package albertodumontt.devRadarAI.infrastructure.crawler;

import albertodumontt.devRadarAI.cli.CliProgress;
import albertodumontt.devRadarAI.model.JobResponse;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class LinkedinWorker {

    // geoId=106057199 = Brazil | f_TPR=r604800 = last 7 days
    private static final String SEARCH_URL =
            "https://www.linkedin.com/jobs/search?keywords=desenvolvedor&geoId=106057199&f_TPR=r604800";

    private static final String BRAVE_PATH =
            "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser";

    private final CliProgress progress;

    public List<JobResponse> crawl() {
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

            progress.update("Connecting to LinkedIn...");
            page.navigate(SEARCH_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            progress.update("Waiting for job listings...");
            page.waitForSelector("ul.jobs-search__results-list", new Page.WaitForSelectorOptions().setTimeout(15_000));

            progress.update("Parsing job cards...");
            List<JobResponse> jobs = page.querySelectorAll("ul.jobs-search__results-list > li").stream()
                    .map(this::parseCard)
                    .filter(Objects::nonNull)
                    .toList();

            int total = jobs.size();
            AtomicInteger counter = new AtomicInteger(1);

            return jobs.stream()
                    .map(job -> {
                        progress.update("Fetching details (" + counter.getAndIncrement() + "/" + total + "): " + job.title());
                        return withDetails(page, job);
                    })
                    .toList();

        } catch (Exception e) {
            progress.update("Crawl failed: " + e.getMessage());
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

            return new JobResponse(title, company, location, null, null, null, false, null, url, publishedAt);

        } catch (Exception ignored) {
            return null;
        }
    }

    private JobResponse withDetails(Page page, JobResponse job) {
        try {
            page.navigate(job.url(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(2_000);

            String workplaceType = Stream.of(
                    "span.job-details-jobs-unified-top-card__workplace-type",
                    "span[class*='workplace-type']",
                    "li.job-details-jobs-unified-top-card__job-insight span"
            )
            .map(page::querySelector)
            .filter(Objects::nonNull)
            .map(el -> el.innerText().trim())
            .filter(t -> !t.isBlank())
            .findFirst()
            .orElse(null);

            Map<String, String> criteria = parseCriteria(page);
            String seniorityLevel = criteria.getOrDefault("seniority level",
                                    criteria.get("nível de experiência"));
            String employmentType = criteria.getOrDefault("employment type",
                                    criteria.get("tipo de emprego"));

            if (workplaceType == null) workplaceType = "notDefined";

            boolean remote = workplaceType.toLowerCase().contains("remote") ||
                             workplaceType.toLowerCase().contains("remoto");

            String description = parseDescription(page);

            return new JobResponse(
                    job.title(), job.company(), job.location(),
                    workplaceType, employmentType, seniorityLevel,
                    remote, description, job.url(), job.publishedAt()
            );

        } catch (Exception ignored) {
            return job;
        }
    }

    private String parseDescription(Page page) {
        String[] selectors = {
            "div.show-more-less-html__markup",
            "div.description__text",
            "section.show-more-less-html"
        };
        for (String selector : selectors) {
            ElementHandle el = page.querySelector(selector);
            if (el == null) continue;
            String text = el.innerText().lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.equalsIgnoreCase("Sobre a vaga") && !line.equalsIgnoreCase("description"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            if (!text.isBlank()) return text;
        }
        return null;
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
