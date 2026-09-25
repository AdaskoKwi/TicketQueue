package pl.kul.fraud_insight_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.kul.fraud_insight_service.model.AnalysisRequest;
import pl.kul.fraud_insight_service.model.kafka.QueueEntry;
import pl.kul.fraud_insight_service.service.FraudInsightService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/fraud-insight")
@RequiredArgsConstructor
public class FraudInsightController {
    private final FraudInsightService fraudService;

    @GetMapping("/{eventId}/analysis")
    public ResponseEntity<?> getDataAnalysisForGivenDay(@PathVariable String eventId, @RequestBody AnalysisRequest req) {
        List<QueueEntry> entries = fraudService.getEventEntryDataForADate(eventId, req.date());
        String analysis = fraudService.getEntryDataAnalysis(entries);

        return ResponseEntity
                .status(200)
                .body(analysis);
    }
}
