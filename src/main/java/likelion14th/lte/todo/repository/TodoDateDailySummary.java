package likelion14th.lte.todo.repository;

import java.time.LocalDate;

public interface TodoDateDailySummary {
    LocalDate getDate();

    long getCompletedCount();

    long getIncompleteCount();
}
