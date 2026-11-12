package likelion14th.lte.statistic.service;

import jakarta.persistence.EntityManager;
import likelion14th.lte.statistic.dto.response.StatisticResponse;
import likelion14th.lte.statistic.entity.Statistic;
import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.repository.TodoDateDailySummary;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class StatisticServiceTest {
    @Test
    void statisticDateUsesKoreaTimeNearUtcDateBoundary() {
        UserRepository userRepository = mock(UserRepository.class);
        TodoDateRepository todoDateRepository = mock(TodoDateRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-11-11T15:30:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        StatisticService service = new StatisticService(
                userRepository,
                todoDateRepository,
                entityManager,
                new StatisticCalculator(),
                clock
        );
        User user = mock(User.class);
        LocalDate koreaDate = LocalDate.of(2026, 11, 12);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(todoDateRepository.summarizeByUserIdUntil(1L, koreaDate)).thenReturn(List.of());

        StatisticResponse response = service.getStatistic(1L);

        assertEquals(0, response.getMonthPercent());
        verify(todoDateRepository).summarizeByUserIdUntil(1L, koreaDate);
    }

    @Test
    void repeatedBatchRecalculationOverwritesInsteadOfIncreasingStoredCounts() {
        UserRepository userRepository = mock(UserRepository.class);
        TodoDateRepository todoDateRepository = mock(TodoDateRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-11-12T03:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        StatisticService service = new StatisticService(
                userRepository,
                todoDateRepository,
                entityManager,
                new StatisticCalculator(),
                clock
        );
        User user = mock(User.class);
        Statistic statistic = Statistic.create();
        LocalDate koreaDate = LocalDate.of(2026, 11, 12);
        TodoDateDailySummary sundaySummary = summary(LocalDate.of(2026, 11, 8), 3, 0);

        when(user.getId()).thenReturn(1L);
        when(user.getStatistic()).thenReturn(statistic);
        when(userRepository.findAll(PageRequest.of(0, 500)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(todoDateRepository.summarizeByUserIdUntil(1L, koreaDate))
                .thenReturn(List.of(sundaySummary));

        service.updateAllStatistics();
        service.updateAllStatistics();

        assertEquals(WeekEnum.SUN, statistic.getMostTodoWeek());
        assertEquals(3, statistic.getStatWeeks().stream()
                .filter(statWeek -> statWeek.getWeek() == WeekEnum.SUN)
                .findFirst()
                .orElseThrow()
                .getCount());
        verify(todoDateRepository, times(2)).summarizeByUserIdUntil(1L, koreaDate);
    }

    private TodoDateDailySummary summary(LocalDate date, long completedCount, long incompleteCount) {
        TodoDateDailySummary summary = mock(TodoDateDailySummary.class);
        when(summary.getDate()).thenReturn(date);
        when(summary.getCompletedCount()).thenReturn(completedCount);
        when(summary.getIncompleteCount()).thenReturn(incompleteCount);
        return summary;
    }
}
