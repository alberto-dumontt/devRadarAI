package albertodumontt.devRadarAI.infrastructure.supabase;

import albertodumontt.devRadarAI.model.JobResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseJobService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int replaceAll(List<JobResponse> jobs) {
        if (jobs.isEmpty()) return 0;

        if (!clearTable()) return 0;

        try {
            List<Map<String, Object>> payload = jobs.stream()
                    .map(this::toMap)
                    .toList();

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/rest/v1/jobs"))
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("Supabase insert error [" + response.statusCode() + "]: " + response.body());
                return 0;
            }

            return jobs.size();
        } catch (Exception e) {
            System.err.println("Failed to insert jobs: " + e.getMessage());
            return 0;
        }
    }

    private boolean clearTable() {
        try {
            // id=not.is.null matches every row (id is always set)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/rest/v1/jobs?id=not.is.null"))
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Prefer", "return=minimal")
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("Supabase delete error [" + response.statusCode() + "]: " + response.body());
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Failed to clear table: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Object> toMap(JobResponse job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", job.title());
        map.put("company", job.company());
        map.put("location", job.location());
        map.put("workplace_type", job.workplaceType().name());
        map.put("employment_type", job.employmentType());
        map.put("seniority_level", job.seniorityLevel());
        map.put("description", job.description());
        map.put("url", job.url());
        map.put("published_at", job.publishedAt() != null ? job.publishedAt().toString() : null);
        return map;
    }
}
