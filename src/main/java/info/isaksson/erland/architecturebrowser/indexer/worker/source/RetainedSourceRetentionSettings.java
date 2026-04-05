package info.isaksson.erland.architecturebrowser.indexer.worker.source;

import java.time.Duration;

public record RetainedSourceRetentionSettings(Duration gitRetentionTtl) {
    public static final String GIT_TTL_HOURS_PROPERTY = "archbrowser.worker.source-retention.git-ttl-hours";
    public static final String GIT_TTL_HOURS_ENV = "ARCH_BROWSER_SOURCE_RETENTION_GIT_TTL_HOURS";
    public static final Duration DEFAULT_GIT_RETENTION_TTL = Duration.ofDays(7);

    public RetainedSourceRetentionSettings {
        if (gitRetentionTtl == null || gitRetentionTtl.isNegative() || gitRetentionTtl.isZero()) {
            throw new IllegalArgumentException("gitRetentionTtl must be > 0");
        }
    }

    public static RetainedSourceRetentionSettings defaults() {
        String configuredHours = configuredGitTtlHours();
        if (configuredHours == null) {
            return new RetainedSourceRetentionSettings(DEFAULT_GIT_RETENTION_TTL);
        }
        try {
            long hours = Long.parseLong(configuredHours.trim());
            if (hours <= 0) {
                throw new IllegalArgumentException("Configured Git retention TTL hours must be > 0");
            }
            return new RetainedSourceRetentionSettings(Duration.ofHours(hours));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Configured Git retention TTL hours must be a whole number: " + configuredHours, exception);
        }
    }

    private static String configuredGitTtlHours() {
        String propertyValue = System.getProperty(GIT_TTL_HOURS_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(GIT_TTL_HOURS_ENV);
        return envValue == null || envValue.isBlank() ? null : envValue;
    }
}
