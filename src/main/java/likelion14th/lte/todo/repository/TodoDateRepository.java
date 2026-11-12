package likelion14th.lte.todo.repository;

import likelion14th.lte.todo.entity.TodoDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface TodoDateRepository extends JpaRepository<TodoDate, Long> {
    //투두랑 - 날짜로 하나 끄집어내는
    Optional<TodoDate> findByTodo_IdAndDateAndCancelledFalse(Long todoId, LocalDate date);

    List<TodoDate> findAllByTodo_User_IdAndDateAndCancelledFalse(Long userId, LocalDate date);

    List<TodoDate> findAllByTodo_IdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    void deleteAllByTodo_IdAndDateGreaterThanEqualAndCompletedFalseAndCancelledFalse(Long todoId, LocalDate from);

    List<TodoDate> findAllByTodo_User_IdAndDateBetweenAndCancelledFalse(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT td.date AS date,
                   SUM(CASE WHEN td.completed = true THEN 1 ELSE 0 END) AS completedCount,
                   SUM(CASE WHEN td.completed = false THEN 1 ELSE 0 END) AS incompleteCount
            FROM TodoDate td
            WHERE td.todo.user.id = :userId
              AND td.cancelled = false
              AND td.date <= :endDate
            GROUP BY td.date
            """)
    List<TodoDateDailySummary> summarizeByUserIdUntil(
            @Param("userId") Long userId,
            @Param("endDate") LocalDate endDate
    );

}
