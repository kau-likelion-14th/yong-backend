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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        return statWeeks.stream()
                .max(Comparator.comparingInt(StatWeek::getCount))
                .map(StatWeek::getWeek)
                .orElse(null);
    }

    public void increaseStreakIfSuccess(boolean success) {
        if (success) {
            this.streak++;
            return;
        }
        this.streak = 0;
    }

    public void increaseWeekCount(LocalDate day) {
        statWeeks.stream()
                .filter(w -> w.getWeek().toDayOfWeek() == day.getDayOfWeek())
                .findFirst()
                .ifPresent(StatWeek::increaseCount);
    }

    public void updateMonthPercent(long completedCount, long incompleteCount) {
        long totalCount = completedCount + incompleteCount;
        if (totalCount == 0) {
            this.monthPercent = 0;
            return;
        }
        this.monthPercent = (int) (completedCount * 100 / totalCount);
    }
}
