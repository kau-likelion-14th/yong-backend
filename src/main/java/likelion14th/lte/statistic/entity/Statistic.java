package likelion14th.lte.statistic.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import likelion14th.lte.Entity.BaseEntity;
import likelion14th.lte.todo.entity.WeekEnum;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Table(name = "statistic")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Statistic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statisticId;

    private int streak;

    private int monthPercent;

    @OneToMany(mappedBy = "statistic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatWeek> statWeeks = new ArrayList<>();

    public static Statistic create() {
        Statistic statistic = new Statistic();
        statistic.streak = 0;
        statistic.monthPercent = 0;
        statistic.initializeWeeks();
        return statistic;
    }

    private void initializeWeeks() {
        for (WeekEnum week : WeekEnum.values()) {
            addStatWeek(StatWeek.create(week));
        }
    }

    private void addStatWeek(StatWeek statWeek) {
        statWeeks.add(statWeek);
        statWeek.setStatistic(this);
    }

    public WeekEnum getMostTodoWeek() {
        int maxCount = statWeeks.stream()
                .mapToInt(StatWeek::getCount)
                .max()
                .orElse(0);
        if (maxCount == 0) {
            return null;
        }

        List<WeekEnum> mostTodoWeeks = statWeeks.stream()
                .filter(statWeek -> statWeek.getCount() == maxCount)
                .map(StatWeek::getWeek)
                .toList();
        return mostTodoWeeks.size() == 1 ? mostTodoWeeks.getFirst() : null;
    }

    public void replace(int streak, int monthPercent, Map<WeekEnum, Integer> completedCountsByWeek) {
        this.streak = streak;
        this.monthPercent = monthPercent;
        statWeeks.forEach(statWeek -> statWeek.updateCount(
                completedCountsByWeek.getOrDefault(statWeek.getWeek(), 0)
        ));
    }
}
