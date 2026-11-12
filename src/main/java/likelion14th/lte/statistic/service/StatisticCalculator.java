package likelion14th.lte.statistic.service;

import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.repository.TodoDateDailySummary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatisticCalculator {
    private static final int MONTH_RANGE_DAYS = 30;

    public StatisticSnapshot calculate(List<TodoDateDailySummary> dailySummaries, LocalDate today) {
        LocalDate monthStartDate = today.minusDays(MONTH_RANGE_DAYS - 1L);
        Map<WeekEnum, Integer> completedCountsByWeek = initializeWeekCounts();
        Map<LocalDate, Boolean> successByDate = new HashMap<>();
        long completedCount = 0;
        long incompleteCount = 0;

        for (TodoDateDailySummary summary : dailySummaries) {
            LocalDate date = summary.getDate();
            if (date.isAfter(today)) {
                continue;
            }

            long dailyCompletedCount = summary.getCompletedCount();
            long dailyIncompleteCount = summary.getIncompleteCount();

            if (!date.isBefore(monthStartDate)) {
                completedCount += dailyCompletedCount;
                incompleteCount += dailyIncompleteCount;
            }

            WeekEnum week = toWeekEnum(date);
            int accumulatedCount = Math.addExact(
                    completedCountsByWeek.get(week),
                    Math.toIntExact(dailyCompletedCount)
            );
            completedCountsByWeek.put(week, accumulatedCount);
            successByDate.put(date, dailyCompletedCount > 0 && dailyIncompleteCount == 0);
        }

        return new StatisticSnapshot(
                calculateStreak(successByDate, today),
                calculateMonthPercent(completedCount, incompleteCount),
                findMostCompletedWeek(completedCountsByWeek),
                completedCountsByWeek
        );
    }

    private Map<WeekEnum, Integer> initializeWeekCounts() {
        Map<WeekEnum, Integer> counts = new EnumMap<>(WeekEnum.class);
        for (WeekEnum week : WeekEnum.values()) {
            counts.put(week, 0);
        }
        return counts;
    }

    private int calculateStreak(Map<LocalDate, Boolean> successByDate, LocalDate today) {
        int streak = 0;
        LocalDate date = today.minusDays(1);

        while (Boolean.TRUE.equals(successByDate.get(date))) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private int calculateMonthPercent(long completedCount, long incompleteCount) {
        long totalCount = completedCount + incompleteCount;
        if (totalCount == 0) {
            return 0;
        }
        return (int) (completedCount * 100.0 / totalCount);
    }

    private WeekEnum findMostCompletedWeek(Map<WeekEnum, Integer> completedCountsByWeek) {
        int maxCount = completedCountsByWeek.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        if (maxCount == 0) {
            return null;
        }

        List<WeekEnum> mostCompletedWeeks = completedCountsByWeek.entrySet().stream()
                .filter(entry -> entry.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();
        return mostCompletedWeeks.size() == 1 ? mostCompletedWeeks.getFirst() : null;
    }

    private WeekEnum toWeekEnum(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> WeekEnum.MON;
            case TUESDAY -> WeekEnum.TUE;
            case WEDNESDAY -> WeekEnum.WED;
            case THURSDAY -> WeekEnum.THU;
            case FRIDAY -> WeekEnum.FRI;
            case SATURDAY -> WeekEnum.SAT;
            case SUNDAY -> WeekEnum.SUN;
        };
    }
}
