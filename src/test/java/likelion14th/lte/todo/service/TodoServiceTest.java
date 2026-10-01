package likelion14th.lte.todo.service;

import likelion14th.lte.category.entity.Category;
import likelion14th.lte.category.repository.CategoryRepository;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.todo.dto.request.TodoCreateRequest;
import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.generator.RoutineTodoDateGenerator;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.todo.repository.TodoRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TodoServiceTest {

    @Test
    void createRoutineTodoRequiresStartDate() {
        TodoRepository todoRepository = mock(TodoRepository.class);
        TodoDateRepository todoDateRepository = mock(TodoDateRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoutineTodoDateGenerator generator = mock(RoutineTodoDateGenerator.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        TodoService service = new TodoService(
                todoRepository,
                todoDateRepository,
                userRepository,
                generator,
                categoryRepository
        );
        TodoCreateRequest request = mock(TodoCreateRequest.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(categoryRepository.findByCategoryName("운동")).thenReturn(Optional.of(mock(Category.class)));
        when(request.getCategoryName()).thenReturn("운동");
        when(request.isRoutineEnabled()).thenReturn(true);
        when(request.getEndDate()).thenReturn(LocalDate.of(2026, 8, 31));
        when(request.getWeek()).thenReturn(WeekEnum.MON);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> service.createTodo(1L, request, null)
        );

        assertEquals(ErrorCode.TODO_ROUTINE_START_DATE_REQUIRED, exception.getCode());
        verify(todoRepository, never()).save(any());
        verifyNoInteractions(generator);
    }

    @Test
    void calendarRequiresExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        TodoDateRepository todoDateRepository = mock(TodoDateRepository.class);
        TodoCalendarService service = new TodoCalendarService(userRepository, todoDateRepository);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> service.getMonthRemainingCounts(999L, 2026, 8)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getCode());
        verifyNoInteractions(todoDateRepository);
    }
}
