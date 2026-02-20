package ru.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationsHandlerImpl implements RecommendationsHandler {
    private final UserActionRepository actionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        Integer limit = request.getMaxResults();

        List<UserAction> userActions = actionRepository.findAllByUserId(userId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp")));
        if (userActions.isEmpty()) {
            return List.of();
        }

        Set<Long> userActionsEventId = userActions.stream().map(UserAction::getEventId).collect(Collectors.toSet());
        List<EventSimilarity> eventSimilarities = similarityRepository
                .findAllByEventAInOrEventBIn(userActionsEventId, userActionsEventId,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score")));

        Set<Long> newEventIds = eventSimilarities.stream()
                .map(EventSimilarity::getEventB)
                .filter(eventId -> !actionRepository.existsByEventIdAndUserId(eventId, userId))
                .collect(Collectors.toSet());

        Set<Long> newEventIdsB = eventSimilarities.stream()
                .map(EventSimilarity::getEventA)
                .filter(eventId -> !actionRepository.existsByEventIdAndUserId(eventId, userId))
                .collect(Collectors.toSet());

        newEventIds.addAll(newEventIdsB);

        return newEventIds.stream()
                .map(id -> RecommendedEventProto.newBuilder()
                        .setEventId(id)
                        .setScore(calculateScore(id, userId, limit))
                        .build())
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();

        List<EventSimilarity> eventSimilaritiesA = similarityRepository.findAllByEventA(eventId,
                PageRequest.of(0, request.getMaxResults(), Sort.by(Sort.Direction.DESC, "score")));
        List<EventSimilarity> eventSimilaritiesB = similarityRepository.findAllByEventB(eventId,
                PageRequest.of(0, request.getMaxResults(), Sort.by(Sort.Direction.DESC, "score")));

        List<RecommendedEventProto> recommendations = new ArrayList<>(eventSimilaritiesA.stream()
                .filter(e -> !actionRepository.existsByEventIdAndUserId(e.getEventB(), userId))
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getEventB())
                        .setScore(e.getScore())
                        .build())
                .toList());

        List<RecommendedEventProto> recommendationsB = eventSimilaritiesB.stream()
                .filter(e -> !actionRepository.existsByEventIdAndUserId(e.getEventA(), userId))
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getEventA())
                        .setScore(e.getScore())
                        .build())
                .toList();

        recommendations.addAll(recommendationsB);

        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(request.getMaxResults())
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        return new ArrayList<>(request.getEventIdList().stream()
                .map(id -> RecommendedEventProto.newBuilder()
                        .setEventId(id)
                        .setScore(actionRepository.countSumWeightByEventId(id))
                        .build())
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .toList());
    }

    private Double calculateScore(Long eventId, Long userId, Integer limit) {
        List<EventSimilarity> eventSimilaritiesA = similarityRepository.findAllByEventA(eventId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score")));

        List<EventSimilarity> eventSimilaritiesB = similarityRepository.findAllByEventB(eventId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score")));

        Map<Long, Double> viewedEventScores = eventSimilaritiesA.stream()
                .filter(e -> actionRepository.existsByEventIdAndUserId(e.getEventB(), userId))
                .collect(Collectors.toMap(EventSimilarity::getEventB, EventSimilarity::getScore));

        Map<Long, Double> viewedEventScoresB = eventSimilaritiesB.stream()
                .filter(e -> actionRepository.existsByEventIdAndUserId(e.getEventA(), userId))
                .collect(Collectors.toMap(EventSimilarity::getEventA, EventSimilarity::getScore));

        viewedEventScores.putAll(viewedEventScoresB);

        Map<Long, Double> actionWeights = actionRepository.findAllByEventIdInAndUserId(viewedEventScores.keySet(),
                        userId).stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getWeight));

        Double sumWeights = (viewedEventScores.entrySet().stream()
                .map(entry -> actionWeights.get(entry.getKey()) * entry.getValue())
                .mapToDouble(Double::doubleValue).sum());

        Double sumScores = (viewedEventScores.values().stream().mapToDouble(Double::doubleValue).sum());

        return sumWeights / sumScores;
    }
}
