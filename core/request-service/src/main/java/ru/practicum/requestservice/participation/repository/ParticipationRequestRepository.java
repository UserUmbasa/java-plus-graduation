package ru.practicum.requestservice.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.requestservice.participation.model.ParticipationRequest;
import ru.practicum.requestservice.participation.model.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByRequesterAndEvent(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequester(Long userId);

    List<ParticipationRequest> findAllByEvent(Long eventId);

    Integer countByEventAndStatus(Long eventId, RequestStatus status);

    @Query("""
            SELECT COUNT(pr)
            FROM ParticipationRequest pr
            WHERE pr.event = :eventId AND pr.status = 'CONFIRMED'""")
    int countConfirmedRequestsForEvent(@Param("eventId") Long eventId);

    @Query("""
            SELECT pr.event as eventId, COUNT(pr) as confirmedCount
            FROM ParticipationRequest pr
            WHERE pr.event IN :eventIds
            AND pr.status = 'CONFIRMED'
            GROUP BY pr.event""")
    List<Object[]> findConfirmedRequestCountsByEventIds(@Param("eventIds") List<Long> eventIds);

}
