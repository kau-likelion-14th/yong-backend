package likelion14th.lte.statistic.service;

import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.repository.TodoDateDailySummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatisticCalculatorTest {
    private final StatisticCalculator calculator = new StatisticCalculator();
    private final LocalDate today = LocalDate.of(2026, 11, 12);

    @Test
    void mostCompletedWeekCountsCompletedTodosEvenWhenTheDayHasIncompleteTodos() {
        StatisticSnapshot snapshot = calculator.calculate(List.of(
                summary(LocalDate.of(2026, 11, 8), 5, 1),
                summary(LocalDate.of(2026, 11, 7), 4, 0)
        ), today);

        assertEquals(WeekEnum.SUN, snapshot.mostTodoWeek());
        assertEquals(90, snapshot.monthPercent());
    }

    @Test
    void monthPercentUsesTodayAndThePreviousTwentyNineDaysOnly() {
        StatisticSnapshot snapshot = calculator.calculate(List.of(
                summary(today.minusDays(29), 1, 0),
                summary(today, 0, 1),
                summary(today.minusDays(30), 100, 0),
                summary(today.plusDays(1), 100, 0)
        ), today);

        assertEquals(50, snapshot.monthPercent());
        assertEquals(101, snapshot.completedCountsByWeek().values().stream()
                .mapToInt(Integer::intValue)
                .sum());
    }

    @Test
    void streakKeepsTheExistingAllTodosCompletedMeaningAndEndsAtYesterday() {
        StatisticSnapshot snapshot = calculator.calculate(List.of(
                summary(today, 1, 0),
                summary(today.minusDays(1), 1, 0),
                summary(today.minusDays(2), 2, 0),
                summary(today.minusDays(3), 1, 1)
        ), today);

        assertEquals(2, snapshot.streak());
    }

    @Test
    void noCompletedTodoOrTiedTopWeeksReturnsNoMostCompletedWeek() {
        StatisticSnapshot noCompletedTodo = calculator.calculate(List.of(
                summary(today, 0, 2)
        ), today);
        StatisticSnapshot tiedTopWeeks = calculator.calculate(List.of(
                summary(LocalDate.of(2026, 11, 7), 1, 0),
                summary(LocalDate.of(2026, 11, 8), 1, 0)
        ), today);

        assertNull(noCompletedTodo.mostTodoWeek());
        assertNull(tiedTopWeeks.mostTodoWeek());
    }

    @Test
    void recalculationReflectsCompletionCancellationWithoutAccumulatingOldCounts() {
        List<TodoDateDailySummary> beforeCancellation = List.of(
                summary(LocalDate.of(2026, 11, 8), 2, 0),
                summary(LocalDate.of(2026, 11, 7), 1, 0)
        );
        List<TodoDateDailySummary> afterCancellation = List.of(
                summary(LocalDate.of(2026, 11, 8), 0, 2),
                summary(LocalDate.of(2026, 11, 7), 1, 0)
        );

        StatisticSnapshot first = calculator.calculate(beforeCancellation, today);
        StatisticSnapshot repeated = calculator.calculate(beforeCancellation, today);
        StatisticSnapshot recalculated = calculator.calculate(afterCancellation, today);

        assertEquals(first, repeated);
        assertEquals(WeekEnum.SUN, first.mostTodoWeek());
        assertEquals(WeekEnum.SAT, recalculated.mostTodoWeek());
        assertEquals(33, recalculated.monthPercent());
    }

    private TodoDateDailySummary summary(LocalDate date, long completedCount, long incompleteCount) {
        return new TodoDateDailySummary() {
            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public long getCompletedCount() {
                return completedCount;
            }

            @Override
            public long getIncompleteCount() {
                return incompleteCount;
            }
        };
    }
}
