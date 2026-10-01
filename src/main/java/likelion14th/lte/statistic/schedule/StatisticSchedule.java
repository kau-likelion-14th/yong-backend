package likelion14th.lte.statistic.schedule;

import likelion14th.lte.statistic.service.StatisticService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StatisticSchedule {
    private final StatisticService statisticService;

    @Scheduled(cron = "0 10 0 * * *")
    public void updateAllStatistics() {
        statisticService.updateAllStatistics();
    }
}
