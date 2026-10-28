package com.slotmind.repository;

import com.slotmind.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Slot> findById(String id);
}