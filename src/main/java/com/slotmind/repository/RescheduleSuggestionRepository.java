package com.slotmind.repository;

import com.slotmind.domain.RescheduleSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import com.slotmind.domain.SuggestionStatus;
import com.slotmind.domain.TriggerReason;

public interface RescheduleSuggestionRepository extends JpaRepository<RescheduleSuggestion, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RescheduleSuggestion> findById(String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RescheduleSuggestion> findByAppointment_IdAndTriggerReasonAndStatus(String appointmentId,
                                                                                   TriggerReason triggerReason,
                                                                                   SuggestionStatus status);
    List<RescheduleSuggestion> findByStatus(SuggestionStatus status);
}