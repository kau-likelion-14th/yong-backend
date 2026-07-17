package likelion14th.lte.todo.service;

import likelion14th.lte.todo.dto.response.TodoCalendarMonthResponse;
import likelion14th.lte.todo.entity.Todo;
import likelion14th.lte.todo.entity.TodoDate;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoCalendarService {
    private final UserRepository userRepository;
    private final TodoDateRepository todoDateRepository;

    @Transactional(readOnly = true)
    public TodoCalendarMonthResponse getMonthRemainingCounts(Long userId,int year,int month) {
        LocalDate startDate = LocalDate.of(year,month,1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<TodoDate> todoDates = todoDateRepository
                .findAllByTodo_User_IdAndDateBetween(userId,startDate,endDate);

        Map<LocalDate, Long> totalCountByDate = todoDates.stream()
                .collect(groupingBy(TodoDate::getDate,counting()));

        Map<LocalDate, Long> remainingCountByDate = todoDates.stream()
                .filter(todoDate -> !todoDate.isCompleted())
                .collect(groupingBy(TodoDate::getDate,counting()));
        List<TodoCalendarMonthResponse.DayInfo> days = new ArrayList<>();

        for(int day=1; day <= endDate.getDayOfMonth(); day++){
            LocalDate date = startDate.withDayOfMonth(day);
            boolean hasTodo = totalCountByDate.containsKey(date);
            long remainingCount = remainingCountByDate.getOrDefault(date,0L);
            days.add(new TodoCalendarMonthResponse.DayInfo(date,remainingCount,hasTodo));
        }

        return TodoCalendarMonthResponse.of(days);

    }
}
