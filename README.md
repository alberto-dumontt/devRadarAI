# DevRadar AI

CLI tool that crawls LinkedIn for developer job listings and persists them to Supabase.

## Tech Stack

- Java 25
- Spring Boot 4
- Playwright (Brave browser, headless)
- Supabase (PostgreSQL via REST API)

## Requirements

- [Brave Browser](https://brave.com) installed at `/Applications/Brave Browser.app`
- Supabase project with the `jobs` table (migration below)

## Setup

Create a `.env` file at the project root:

```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_KEY=your-service-role-key
```

> Use the **service_role** key so the app can bypass RLS and write to the table.

### Database migration

```sql
create table if not exists jobs (
  id               uuid primary key default gen_random_uuid(),
  title            text not null,
  company          text not null,
  location         text,
  workplace_type   text,
  employment_type  text,
  seniority_level  text,
  published_at     timestamptz,
  url              text,
  description      text,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

alter table jobs enable row level security;

create policy "jobs: public read"
  on jobs for select using (true);

create policy "jobs: authenticated insert"
  on jobs for insert
  with check (auth.role() = 'authenticated');

create policy "jobs: authenticated update"
  on jobs for update
  using (auth.role() = 'authenticated')
  with check (auth.role() = 'authenticated');

create policy "jobs: authenticated delete"
  on jobs for delete
  using (auth.role() = 'authenticated');

create trigger jobs_updated_at
  before update on jobs
  for each row execute procedure handle_updated_at();
```

## Run

```bash
./mvnw spring-boot:run
```

The spinner shows progress while crawling and persisting:

```
  Connecting to LinkedIn...
  Fetching details (12/24): Desenvolvedor Java Sênior
  Saving to database...
```

When done, results are printed:

```
=== DevRadar AI — 24 jobs found, 24 persisted ===

────────────────────────────────────────────────────────────
  Desenvolvedor Java Sênior
  Acme Corp • Brasil

  Workplace  : REMOTE
  Type       : Tempo integral
  Seniority  : Pleno-sênior
  Published  : 2026-05-08
  URL        : https://www.linkedin.com/jobs/view/...

  Description:
    Buscamos um desenvolvedor Java com experiência em...
```

## Behavior

- Each run **clears the table and reinserts** all crawled jobs.
- If the crawl returns no results, **the database is left untouched**.

## Search criteria

| Parameter | Value |
|-----------|-------|
| Keywords  | `desenvolvedor` |
| Location  | Brazil (`geoId=106057199`) |
| Period    | Last 7 days |

## Project structure

```
src/main/java/albertodumontt/devRadarAI/
├── DevRadarAiApplication.java        # CommandLineRunner — orchestrates crawl + persistence
├── cli/
│   └── CliProgress.java              # Animated spinner
├── model/
│   ├── JobResponse.java              # record: all job fields
│   └── WorkplaceType.java            # enum: REMOTE, HYBRID, ON_SITE, NOT_DEFINED
└── infrastructure/
    ├── crawler/
    │   └── LinkedinWorker.java       # Playwright crawler — search list + detail pages
    └── supabase/
        └── SupabaseJobService.java   # Supabase REST client — clear + batch insert
```

## Notes

- LinkedIn blocks unauthenticated HTTP requests (HTTP 429). Playwright with a real browser avoids this.
- Fields that LinkedIn does not provide are set to `NOT_DEFINED`.
