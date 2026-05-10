package albertodumontt.devRadarAI.model;

public enum WorkplaceType {
    REMOTE, HYBRID, ON_SITE, NOT_DEFINED;

    public static WorkplaceType from(String raw) {
        if (raw == null) return NOT_DEFINED;
        String value = raw.toLowerCase().trim();
        if (value.contains("remoto") || value.contains("remote")) return REMOTE;
        if (value.contains("híbrido") || value.contains("hibrido") || value.contains("hybrid")) return HYBRID;
        if (value.contains("presencial") || value.contains("on-site") || value.contains("onsite")) return ON_SITE;
        return NOT_DEFINED;
    }
}
