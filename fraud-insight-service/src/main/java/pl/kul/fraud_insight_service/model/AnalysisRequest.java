package pl.kul.fraud_insight_service.model;

import java.time.LocalDate;

public record AnalysisRequest(
        LocalDate date
) {
}
