package albertodumontt.devRadarAI.model;

import java.time.LocalDate;

public record JobResponse(
        String title,
        String company,
        String location,
        String workplaceType,
        String employmentType,
        String seniorityLevel,
        boolean remote,
        String url,
        LocalDate publishedAt
) {}
