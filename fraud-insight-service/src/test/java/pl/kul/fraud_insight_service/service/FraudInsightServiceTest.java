package pl.kul.fraud_insight_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kul.fraud_insight_service.model.kafka.QueueEntry;
import pl.kul.fraud_insight_service.repository.QueueEntryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pl.kul.fraud_insight_service.data.QueueEntryTestData.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FraudInsightServiceTest {
    @Mock
    private QueueEntryRepository repository;

    @InjectMocks
    private FraudInsightService fraudService;

    @BeforeEach
    public void setup() {
       repository.deleteAll();
    }

    @Test
    public void saveQueueJoinEvent_should_return_optional_containing_queue_entry() {
        // given
        when(repository.existsById(KAFKA_EVENT_UUID)).thenReturn(false);
        when(repository.save(any(QueueEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Optional<QueueEntry> result = fraudService.saveQueueJoinEvent(VALID_QUEUE_JOINED_EVENT);

        // then
        assertThat(result).isPresent();

        QueueEntry entry = result.get();

        assertThat(entry.getEventId()).isEqualTo(EVENT_ID);
        assertThat(entry.getUserId()).isEqualTo(USER_ID);
        assertThat(entry.getKafkaEventUUID()).isEqualTo(KAFKA_EVENT_UUID);
    }

    @Test
    public void saveQueueJoinEvent_should_return_empty_optional_when_entry_already_exists() {
        // given
        when(repository.existsById(KAFKA_EVENT_UUID)).thenReturn(true);

        // when
        Optional<QueueEntry> result = fraudService.saveQueueJoinEvent(VALID_QUEUE_JOINED_EVENT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void getEventEntryDataForADate_should_return_empty_list_for_no_data_found() {
        // given
        LocalDate date = LocalDate.of(2026, 9, 22);
        ZoneId zoneId = ZoneId.of("UTC");

        Instant from = date.atStartOfDay(zoneId).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        when(repository.findAllByEventIdAndJoinedAtBetween(EVENT_ID, from, to)).thenReturn(List.of());

        // when
        List<QueueEntry> result = fraudService.getEventEntryDataForADate(EVENT_ID, date);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void getEventEntryDataForADate_should_return_list_with_2_items_when_theres_2_entries() {
        // given
        LocalDate date = LocalDate.of(2026, 9, 22);
        ZoneId zoneId = ZoneId.of("UTC");

        Instant from = date.atStartOfDay(zoneId).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        when(repository.findAllByEventIdAndJoinedAtBetween(EVENT_ID, from, to)).thenReturn(
                List.of(VALID_QUEUE_ENTRY, VALID_QUEUE_ENTRY_TWO)
        );

        // when
        List<QueueEntry> result = fraudService.getEventEntryDataForADate(EVENT_ID, date);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.getFirst().getKafkaEventUUID()).isEqualTo(KAFKA_EVENT_UUID);
        assertThat(result.getFirst().getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getFirst().getUserId()).isEqualTo(USER_ID);

        assertThat(result.get(1).getKafkaEventUUID()).isEqualTo(KAFKA_EVENT_UUID_TWO);
        assertThat(result.get(1).getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.get(1).getUserId()).isEqualTo(USER_ID_TWO);
    }
}