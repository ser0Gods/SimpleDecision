package de.blumenau.template.repository;

import de.blumenau.template.domain.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    List<AnswerRecord> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}
