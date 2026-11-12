package likelion14th.lte.statistic.dto.response;

import likelion14th.lte.statistic.service.StatisticSnapshot;
import likelion14th.lte.todo.entity.WeekEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StatisticResponse {
    private int streak;
    private int monthPercent;
    private WeekEnum mostTodoWeek;

    public static StatisticResponse from(StatisticSnapshot snapshot) {
        return new StatisticResponse(
                snapshot.streak(),
                snapshot.monthPercent(),
                snapshot.mostTodoWeek()
        );
    }
}
