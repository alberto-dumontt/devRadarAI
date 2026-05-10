package albertodumontt.devRadarAI.model;

import java.time.LocalDate;

public record JobResponse(
        String title,
        String company,
        String location,
        WorkplaceType workplaceType,
        String employmentType,
        String seniorityLevel,
        String description,
        String url,
        LocalDate publishedAt
) {}
