package albertodumontt.devRadarAI;

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

	public static void main(String[] args) {
		SpringApplication.run(DevRadarAiApplication.class, args);
	}

	@Override
	public void run(String... args) {
		Thread spinner = buildSpinner();
		spinner.start();

		List<JobResponse> jobs = linkedinWorker.crawl();

		spinner.interrupt();
		System.out.print("\r" + " ".repeat(50) + "\r");

		System.out.println("=== DevRadar AI — " + jobs.size() + " jobs found ===\n");

		jobs.forEach(job -> System.out.printf(
				"[%s] %s @ %s%n  Workplace : %s%n  Type      : %s%n  Seniority : %s%n  Location  : %s%n  URL       : %s%n%n",
				job.publishedAt(),
				job.title(),
				job.company(),
				job.workplaceType(),
				job.employmentType(),
				job.seniorityLevel(),
				job.location(),
				job.url()
		));
	}

	private Thread buildSpinner() {
		String[] frames = {"|", "/", "-", "\\"};
		Thread thread = new Thread(() -> {
			int i = 0;
			while (!Thread.currentThread().isInterrupted()) {
				System.out.print("\r  Crawling LinkedIn... " + frames[i % frames.length]);
				System.out.flush();
				i++;
				try {
					Thread.sleep(120);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		thread.setDaemon(true);
		return thread;
	}
}
