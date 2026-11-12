package likelion14th.lte.statistic.entity;

import likelion14th.lte.todo.entity.WeekEnum;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatisticTest {
    @Test
    void replaceOverwritesPreviouslyAccumulatedStatisticValues() {
        Statistic statistic = Statistic.create();

        statistic.replace(9, 25, counts(0, 8));
        statistic.replace(2, 75, counts(4, 1));
        statistic.replace(2, 75, counts(4, 1));

        assertEquals(2, statistic.getStreak());
        assertEquals(75, statistic.getMonthPercent());
        assertEquals(WeekEnum.SUN, statistic.getMostTodoWeek());
        assertEquals(4, countOf(statistic, WeekEnum.SUN));
        assertEquals(1, countOf(statistic, WeekEnum.SAT));
    }

    @Test
    void storedStatisticDoesNotPickAnArbitraryWeekForZeroOrTiedCounts() {
        Statistic statistic = Statistic.create();

        assertNull(statistic.getMostTodoWeek());

        statistic.replace(0, 0, counts(2, 2));

        assertNull(statistic.getMostTodoWeek());
    }

    private Map<WeekEnum, Integer> counts(int sundayCount, int saturdayCount) {
        Map<WeekEnum, Integer> counts = new EnumMap<>(WeekEnum.class);
        for (WeekEnum week : WeekEnum.values()) {
            counts.put(week, 0);
        }
        counts.put(WeekEnum.SUN, sundayCount);
        counts.put(WeekEnum.SAT, saturdayCount);
        return counts;
    }

    private int countOf(Statistic statistic, WeekEnum week) {
        return statistic.getStatWeeks().stream()
                .filter(statWeek -> statWeek.getWeek() == week)
                .findFirst()
                .orElseThrow()
                .getCount();
    }
}
