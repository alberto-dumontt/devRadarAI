package albertodumontt.devRadarAI;

import albertodumontt.devRadarAI.cli.CliProgress;
import albertodumontt.devRadarAI.infrastructure.crawler.LinkedinWorker;
import albertodumontt.devRadarAI.model.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@RequiredArgsConstructor
public class DevRadarAiApplication implements CommandLineRunner {

	private final LinkedinWorker linkedinWorker;
	private final CliProgress progress;

	public static void main(String[] args) {
		SpringApplication.run(DevRadarAiApplication.class, args);
	}

	@Override
	public void run(String... args) {
		progress.start("Starting up...");

		List<JobResponse> jobs = linkedinWorker.crawl();

		progress.stop();

		System.out.println("=== DevRadar AI — " + jobs.size() + " jobs found ===\n");

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
			if (job.description() != null) {
				job.description().lines().forEach(line -> System.out.println("    " + line));
			} else {
				System.out.println("    -");
			}
			System.out.println();
		});
	}
}
