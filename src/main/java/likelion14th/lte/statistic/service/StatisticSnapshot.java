package likelion14th.lte.statistic.service;

import likelion14th.lte.todo.entity.WeekEnum;

import java.util.Map;

public record StatisticSnapshot(
        int streak,
        int monthPercent,
        WeekEnum mostTodoWeek,
        Map<WeekEnum, Integer> completedCountsByWeek
) {
    public StatisticSnapshot {
        completedCountsByWeek = Map.copyOf(completedCountsByWeek);
    }
}
