package com.slotmind.repository;

import com.slotmind.domain.Appointment;
import com.slotmind.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByProvider_IdAndStatus(String providerId, AppointmentStatus status);
    Optional<Appointment> findBySlot_Id(String slotId);
}