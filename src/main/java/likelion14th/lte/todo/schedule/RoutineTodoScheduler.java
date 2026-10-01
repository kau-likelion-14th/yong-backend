package likelion14th.lte.todo.schedule;

import likelion14th.lte.todo.entity.Todo;
import likelion14th.lte.todo.generator.RoutineTodoDateGenerator;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.todo.repository.TodoRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineTodoScheduler {

    private final RoutineTodoDateGenerator routineTodoDateGenerator;
    private final TodoRepository todoRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void generateRoutineTodoDates() {
        LocalDate today = LocalDate.now();

        List<Todo> routineTodos = todoRepository.findAllByRoutineEnabledTrue();
        for(Todo todo : routineTodos) {
            routineTodoDateGenerator.generate(
                    todo,
                    todo.getStartDate(),
                    todo.getEndDate(),
                    today
            );
        }
    }
}
