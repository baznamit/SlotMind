package com.slotmind.config;

import com.slotmind.domain.*;
import com.slotmind.repository.AppointmentRepository;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.repository.SlotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;

@Configuration
public class SeedDataConfiguration {
    @Bean
    CommandLineRunner seedData(ProviderRepository providers, SlotRepository slots, AppointmentRepository appointments) {
        return args -> {
            if (providers.count() > 0 || slots.count() > 0 || appointments.count() > 0) return;
            var now = LocalDateTime.now();
            var p1 = providers.save(new Provider("PRV-001", "Dr. Sarah Chen", Specialty.GENERAL, ProviderStatus.BUSY));
            var p2 = providers.save(new Provider("PRV-002", "Dr. James Okonkwo", Specialty.SPECIALIST, ProviderStatus.AVAILABLE));
            var p3 = providers.save(new Provider("PRV-003", "Dr. Priya Sharma", Specialty.PEDIATRIC, ProviderStatus.BUSY));
            var p4 = providers.save(new Provider("PRV-004", "Dr. Rahul Verma", Specialty.GENERAL, ProviderStatus.AVAILABLE));
            var p5 = providers.save(new Provider("PRV-005", "Dr. Ananya Iyer", Specialty.SPECIALIST, ProviderStatus.BUSY));
            var s1 = slots.save(new Slot("SLT-001", now.plusMinutes(60), 30, p1, SlotStatus.BOOKED));
            var s2 = slots.save(new Slot("SLT-002", now.plusMinutes(120), 30, p1, SlotStatus.BOOKED));
            var s3 = slots.save(new Slot("SLT-003", now.plusMinutes(90), 45, p3, SlotStatus.BOOKED));
            slots.save(new Slot("SLT-004", now.plusMinutes(180), 30, p2, SlotStatus.OPEN));
            slots.save(new Slot("SLT-005", now.plusMinutes(150), 30, p4, SlotStatus.OPEN));
            var s6 = slots.save(new Slot("SLT-006", now.plusMinutes(240), 60, p5, SlotStatus.BOOKED));
            var s7 = slots.save(new Slot("SLT-007", now.plusMinutes(210), 30, p1, SlotStatus.OPEN));
            var s8 = slots.save(new Slot("SLT-008", now.plusMinutes(270), 30, p3, SlotStatus.OPEN));
            var s9 = slots.save(new Slot("SLT-009", now.plusMinutes(300), 30, p2, SlotStatus.OPEN));
            var s10 = slots.save(new Slot("SLT-010", now.plusMinutes(330), 45, p4, SlotStatus.OPEN));
            var s11 = slots.save(new Slot("SLT-011", now.plusMinutes(360), 30, p5, SlotStatus.OPEN));
            var s12 = slots.save(new Slot("SLT-012", now.plusMinutes(390), 30, p1, SlotStatus.OPEN));
            var s13 = slots.save(new Slot("SLT-013", now.plusMinutes(420), 45, p3, SlotStatus.OPEN));
            var s14 = slots.save(new Slot("SLT-014", now.plusMinutes(450), 30, p2, SlotStatus.OPEN));
            var s15 = slots.save(new Slot("SLT-015", now.plusMinutes(480), 30, p4, SlotStatus.OPEN));
            var s16 = slots.save(new Slot("SLT-016", now.plusMinutes(510), 60, p5, SlotStatus.OPEN));
            var s17 = slots.save(new Slot("SLT-017", now.plusMinutes(540), 30, p1, SlotStatus.OPEN));
            var s18 = slots.save(new Slot("SLT-018", now.plusMinutes(570), 45, p3, SlotStatus.OPEN));
            appointments.save(new Appointment("APT-001", "John Martinez", AppointmentType.ROUTINE, p1, s1, AppointmentStatus.CONFIRMED));
            appointments.save(new Appointment("APT-002", "Emily Watson", AppointmentType.FOLLOW_UP, p1, s2, AppointmentStatus.CONFIRMED));
            appointments.save(new Appointment("APT-003", "Aisha Patel", AppointmentType.URGENT, p3, s3, AppointmentStatus.CONFIRMED));
            appointments.save(new Appointment("APT-004", "Robert Kim", AppointmentType.ROUTINE, p5, s6, AppointmentStatus.CONFIRMED));
            var s19 = slots.save(new Slot("SLT-019", now.plusMinutes(600), 30, p4, SlotStatus.BOOKED));
            var s20 = slots.save(new Slot("SLT-020", now.plusMinutes(630), 30, p2, SlotStatus.BOOKED));
            var s21 = slots.save(new Slot("SLT-021", now.plusMinutes(660), 45, p3, SlotStatus.BOOKED));
            appointments.save(new Appointment("APT-005", "Daniel Lewis", AppointmentType.FOLLOW_UP, p4, s19, AppointmentStatus.CONFIRMED));
            appointments.save(new Appointment("APT-006", "Sofia Anderson", AppointmentType.URGENT, p2, s20, AppointmentStatus.CONFIRMED));
            appointments.save(new Appointment("APT-007", "Michael Brown", AppointmentType.ROUTINE, p3, s21, AppointmentStatus.CONFIRMED));
        };
    }
}