package likelion14th.lte.todo.repository;

import likelion14th.lte.todo.entity.TodoDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.*;

public interface TodoDateRepository extends JpaRepository<TodoDate, Long> {
    //투두랑 - 날짜로 하나 끄집어내는
    Optional<TodoDate> findByTodo_IdAndDate(Long todoId, LocalDate date);

    List<TodoDate> findAllByTodo_User_IdAndDate(Long userId, LocalDate date);

    List<TodoDate> findAllByTodo_IdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    void deleteAllByTodo_IdAndDateGreaterThanEqual(Long todoId, LocalDate from);

    List<TodoDate> findAllByTodo_User_IdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

}
