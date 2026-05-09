# DevRadar AI

CLI tool that crawls LinkedIn and prints developer job listings directly in the terminal.

## Tech Stack

- Java 25
- Spring Boot 4
- Playwright (Brave browser, headless)

## Requirements

- [Brave Browser](https://brave.com) installed at `/Applications/Brave Browser.app`

## Run

```bash
./mvnw spring-boot:run
```

While crawling, a spinner is shown:

```
  Crawling LinkedIn... /
```

When done, results are printed:

```
=== DevRadar AI — 24 jobs found ===

[2026-05-07] Desenvolvedor Java Sênior @ Acme Corp
  Workplace : Remoto
  Type      : Tempo integral
  Seniority : Pleno-sênior
  Location  : Brasil
  URL       : https://www.linkedin.com/jobs/view/...

[2026-05-08] Engenheiro de Software @ Betha Sistemas
  Workplace : Híbrido
  Type      : Tempo integral
  Seniority : Júnior
  Location  : Blumenau, Santa Catarina, Brasil
  URL       : https://www.linkedin.com/jobs/view/...
```

## Search criteria (hardcoded)

| Parameter | Value |
|-----------|-------|
| Keywords  | `desenvolvedor` |
| Location  | Brazil (`geoId=106057199`) |
| Period    | Last 7 days |

## Project structure

```
src/main/java/albertodumontt/devRadarAI/
├── DevRadarAiApplication.java   # CommandLineRunner — spinner + output
├── model/
│   └── JobResponse.java         # record: title, company, location, workplaceType, employmentType, seniorityLevel, remote, url, publishedAt
└── infrastructure/
    └── crawler/
        └── LinkedinWorker.java  # Playwright crawler — search list + detail pages
```

## Notes

- LinkedIn blocks unauthenticated HTTP requests (HTTP 429). Playwright with a real browser avoids this.
- Job descriptions are not available — LinkedIn detail pages require an authenticated session to render the full content.
