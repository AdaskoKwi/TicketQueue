package pl.kul.fraud_insight_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;
import pl.kul.fraud_insight_service.model.kafka.QueueEntry;
import pl.kul.fraud_insight_service.model.kafka.QueueJoinedEvent;
import pl.kul.fraud_insight_service.repository.QueueEntryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FraudInsightService {
    private final QueueEntryRepository repository;
    private final ChatModel chatModel;

    public Optional<QueueEntry> saveQueueJoinEvent(QueueJoinedEvent event) {
        if (repository.existsById(event.kafkaEventUUID())) {
            return Optional.empty();
        }

        return Optional.of(repository.save(new QueueEntry(event)));
    }

    public List<QueueEntry> getEventEntryDataForADate(String eventId, LocalDate date) {
        ZoneId zoneId = ZoneId.of("UTC");

        Instant from = date.atStartOfDay(zoneId).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        return repository.findAllByEventIdAndJoinedAtBetween(eventId, from, to);
    }

    public String getEntryDataAnalysis(List<QueueEntry> queueEntries) {
        String contents = """
                You are an anomaly detection system.
                
                    Analyze the queue entries provided below.
                
                    Look for anomalies such as:
                    - duplicate Kafka event UUIDs
                    - duplicate users for the same event
                    - suspicious timestamps
                    - users joining the queue in an unusual order
                    - any other suspicious or inconsistent data
                
                    IMPORTANT:
                    Do NOT return the input data.
                    Return ONLY your analysis.
                    Do NOT make up anomalies if there are none.
                
                    Return JSON in exactly this format:
                
                    {
                      "anomalyDetected": true,
                      "anomalies": [
                        {
                          "type": "ANOMALY_TYPE",
                          "description": "Description of the anomaly",
                          "affectedUserIds": ["user-1"]
                        }
                      ]
                    }
                
                    Only if there are no anomalies, return:
                
                    {
                      "anomalyDetected": false,
                      "anomalies": []
                    }
                
                
                List:
                %s
                """.formatted(queueEntries);

        ChatResponse response = chatModel.call(
                new Prompt(
                        contents,
                        OllamaChatOptions.builder()
                                .model("phi4-mini")
                                .format("json")
                                .disableThinking().build()
                )
        );

        return response.getResult().getOutput().getText();
    }
}
