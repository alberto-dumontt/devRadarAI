package albertodumontt.devRadarAI;

import albertodumontt.devRadarAI.cli.CliProgress;
import albertodumontt.devRadarAI.infrastructure.crawler.LinkedinWorker;
import albertodumontt.devRadarAI.infrastructure.supabase.SupabaseJobService;
import albertodumontt.devRadarAI.model.JobResponse;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
public class DevRadarAiApplication implements CommandLineRunner {

	private final LinkedinWorker linkedinWorker;
	private final SupabaseJobService supabaseJobService;
	private final CliProgress progress;

	public static void main(String[] args) {
		Dotenv.configure().ignoreIfMissing().load().entries()
				.forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		SpringApplication.run(DevRadarAiApplication.class, args);
	}

	@Override
	public void run(String... args) {
		progress.start("Starting up...");

		List<JobResponse> jobs = linkedinWorker.crawl();

		if (jobs.isEmpty()) {
			progress.stop();
			System.out.println("No jobs found — database unchanged.");
			return;
		}

		progress.update("Saving to database...");
		int saved = supabaseJobService.replaceAll(jobs);

		progress.stop();

		System.out.println("=== DevRadar AI — " + jobs.size() + " jobs found, " + saved + " persisted ===\n");

		jobs.forEach(job -> {
			String separator = "─".repeat(60);
			System.out.println(separator);
			System.out.println("  " + job.title());
			System.out.println("  " + job.company() + " • " + job.location());
			System.out.println();
			System.out.println("  Workplace  : " + job.workplaceType());
			System.out.println("  Type       : " + job.employmentType());
			System.out.println("  Seniority  : " + job.seniorityLevel());
			System.out.println("  Published  : " + job.publishedAt());
			System.out.println("  URL        : " + job.url());
			System.out.println();
			System.out.println("  Description:");
			job.description().lines().forEach(line -> System.out.println("    " + line));
			System.out.println();
		});
	}
}
