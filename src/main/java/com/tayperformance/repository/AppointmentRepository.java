package com.tayperformance.repository;

import com.tayperformance.entity.Appointment;
import com.tayperformance.entity.AppointmentStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
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
        WHERE (:term IS NULL OR :term = '' OR
              LOWER(a.customer.phone) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(a.customer.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(a.customer.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(a.carBrand) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(a.carModel) LIKE LOWER(CONCAT('%', :term, '%')))
        ORDER BY a.startTime DESC
    """)
    Page<Appointment> search(@Param("term") String term, Pageable pageable);

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = com.tayperformance.entity.AppointmentStatus.COMPLETED
          AND a.startTime >= :since
        ORDER BY a.startTime DESC
    """)
    List<Appointment> findCompletedByCustomerSince(@Param("customerId") Long customerId,
                                                   @Param("since") OffsetDateTime since);

    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = com.tayperformance.entity.AppointmentStatus.NOSHOW
          AND a.startTime >= :since
    """)
    long countNoShowsByCustomerSince(@Param("customerId") Long customerId,
                                     @Param("since") OffsetDateTime since);

    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.customer.id = :customerId
          AND a.status = com.tayperformance.entity.AppointmentStatus.COMPLETED
    """)
    long countCompletedByCustomer(@Param("customerId") Long customerId);

    @Query("""
select a
from Appointment a
join a.customer c
where (
  :q is null or :q = '' or
  lower(a.description) like lower(concat('%', :q, '%')) or
  lower(a.carBrand) like lower(concat('%', :q, '%')) or
  lower(c.phone) like lower(concat('%', :q, '%'))
)
and (
  :from is null or :to is null or (a.startTime >= :from and a.startTime < :to)
)
order by a.startTime asc
""")
    Page<Appointment> searchAdvanced(
            @Param("q") String q,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

}
