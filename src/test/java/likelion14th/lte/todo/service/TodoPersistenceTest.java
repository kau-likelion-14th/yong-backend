package likelion14th.lte.todo.service;

import likelion14th.lte.category.entity.Category;
import likelion14th.lte.category.repository.CategoryRepository;
import likelion14th.lte.todo.dto.request.TodoUpdateRequest;
import likelion14th.lte.todo.entity.Todo;
import likelion14th.lte.todo.entity.TodoDate;
import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.generator.RoutineTodoDateGenerator;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.todo.repository.TodoRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TodoPersistenceTest {
    private TodoRepository todos;
    private TodoDateRepository dates;
    private RoutineTodoDateGenerator generator;
    private TodoService service;
    private Todo todo;
    private final LocalDate today = LocalDate.of(2026, 11, 12);

    @BeforeEach
    void setUp() {
        todos = mock(TodoRepository.class);
        dates = mock(TodoDateRepository.class);
        generator = mock(RoutineTodoDateGenerator.class);
        UserRepository users = mock(UserRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        User user = mock(User.class);
        Category category = mock(Category.class);
        when(user.getId()).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(categories.findByCategoryName("공부")).thenReturn(Optional.of(category));
        todo = Todo.create(user, "Original", true, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), category, WeekEnum.THU);
        ReflectionTestUtils.setField(todo, "id", 10L);
        when(todos.findById(10L)).thenReturn(Optional.of(todo));
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T15:30:00Z"), ZoneId.of("Asia/Seoul"));
        service = new TodoService(todos, dates, users, generator, categories, clock);
    }

    private TodoUpdateRequest request(WeekEnum week) {
        TodoUpdateRequest request = mock(TodoUpdateRequest.class);
        when(request.getDescription()).thenReturn("Updated");
        when(request.getCategoryName()).thenReturn("공부");
        when(request.isRoutineEnabled()).thenReturn(true);
        when(request.getStartDate()).thenReturn(todo.getStartDate());
        when(request.getEndDate()).thenReturn(todo.getEndDate());
        when(request.getWeek()).thenReturn(week);
        return request;
    }

    @Test
    void changingOnlyTextDoesNotRecreateDatesOrResetCompletedRecords() {
        TodoDate completed = TodoDate.create(todo, today);
        completed.setCompleted(true);
        var response = service.updateTodoDetail(1L, 10L, request(WeekEnum.THU));
        assertEquals("Updated", response.getDescription());
        assertTrue(completed.isCompleted());
        verifyNoInteractions(dates, generator);
    }

    @Test
    void changingScheduleOnlyRemovesUnfinishedUncancelledFutureDatesUsingKoreaToday() {
        service.updateTodoDetail(1L, 10L, request(WeekEnum.SUN));
        verify(dates).deleteAllByTodo_IdAndDateGreaterThanEqualAndCompletedFalseAndCancelledFalse(10L, today);
        verify(generator).generate(todo, todo.getStartDate(), todo.getEndDate(), today);
    }

    @Test
    void deletingRoutineOccurrenceKeepsAnExclusionAndTheGeneratorDoesNotRecreateIt() {
        TodoDate occurrence = TodoDate.create(todo, today);
        when(dates.findByTodo_IdAndDateAndCancelledFalse(10L, today)).thenReturn(Optional.of(occurrence));
        service.deleteTodo(1L, 10L, today);
        assertTrue(occurrence.isCancelled());
        verify(dates, never()).delete(any());
        verify(todos, never()).delete(any());
        when(dates.findAllByTodo_IdAndDateBetween(10L, today, today)).thenReturn(List.of(occurrence));
        new RoutineTodoDateGenerator(dates) {}.generate(todo, today, today, today);
        verify(dates).saveAll(argThat(values -> !values.iterator().hasNext()));
    }
}
