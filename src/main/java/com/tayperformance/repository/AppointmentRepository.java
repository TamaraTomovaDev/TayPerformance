package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status IN :statuses
          AND (:staffId IS NULL OR a.assignedStaff.id = :staffId)
          AND a.startTime < :endTime
          AND a.endTime > :startTime
          AND (:excludeId IS NULL OR a.id != :excludeId)
        ORDER BY a.startTime ASC
    """)
    List<Appointment> findConflicting(
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("staffId") Long staffId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    Page<Appointment> findAllByOrderByStartTimeDesc(Pageable pageable);

    @Query("""
        SELECT a FROM Appointment a
        WHERE LOWER(a.customer.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.customer.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.customer.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.carBrand) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(a.carModel) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY a.startTime DESC
    """)
    Page<Appointment> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
    SELECT a FROM Appointment a
    WHERE a.customer.id = :customerId
      AND a.status = 'COMPLETED'
      AND a.startTime >= :since
    ORDER BY a.startTime DESC
""")
    List<Appointment> findCompletedByCustomerSince(
            @Param("customerId") Long customerId,
            @Param("since") OffsetDateTime since
    );

    @Query("""
    SELECT COUNT(a) FROM Appointment a
    WHERE a.customer.id = :customerId
      AND a.status = 'NOSHOW'
      AND a.startTime >= :since
""")
    long countNoShowsByCustomerSince(
            @Param("customerId") Long customerId,
            @Param("since") OffsetDateTime since
    );

    @Query("""
    SELECT COUNT(a) FROM Appointment a
    WHERE a.customer.id = :customerId
      AND a.status = 'COMPLETED'
""")
    long countCompletedByCustomer(@Param("customerId") Long customerId);

}
