# DevRadar AI

Job aggregation platform for developers. Crawls LinkedIn and returns active job listings via a REST API.

## Tech Stack

- Java 25
- Spring Boot 4
- Jsoup (web crawling)
- SpringDoc OpenAPI (Swagger UI)

## Getting Started

```bash
./mvnw spring-boot:run
```

API available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui/index.html`

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/jobs` | Crawls LinkedIn and returns developer jobs (Brazil, last 7 days) |

### Response example

```json
[
  {
    "title": "Desenvolvedor Java Sênior",
    "company": "Acme Corp",
    "location": "São Paulo, Brasil",
    "remote": false,
    "url": "https://www.linkedin.com/jobs/view/...",
    "publishedAt": "2026-05-07"
  }
]
```

## Project Structure

```
src/main/java/albertodumontt/devRadarAI/
├── model/
│   └── JobResponse.java          # API response record
├── infrastructure/
│   ├── config/
│   │   ├── SecurityConfig.java   # Spring Security (stateless, public API)
│   │   └── OpenApiConfig.java    # Swagger configuration
│   └── crawler/
│       └── LinkedinWorker.java   # Jsoup-based LinkedIn crawler
└── infrastructure/controller/
    └── JobController.java        # GET /api/v1/jobs
```

## Notes

- LinkedIn detail pages return HTTP 429 for unauthenticated scrapers, so job descriptions are not available via scraping.
- For production use, consider the official LinkedIn Jobs API or an authenticated browser session (Selenium/Playwright).
