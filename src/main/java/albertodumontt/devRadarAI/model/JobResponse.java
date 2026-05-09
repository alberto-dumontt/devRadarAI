package albertodumontt.devRadarAI.model;

import java.time.LocalDate;

public record JobResponse(
        String title,
        String company,
        String location,
        boolean remote,
        String url,
        LocalDate publishedAt
) {}
