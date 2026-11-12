package likelion14th.lte.todo.service;

import jakarta.persistence.EntityManager;
import likelion14th.lte.category.entity.Category;
import likelion14th.lte.category.repository.CategoryRepository;
import likelion14th.lte.follow.entity.Follow;
import likelion14th.lte.login.domain.RefreshToken;
import likelion14th.lte.statistic.entity.Statistic;
import likelion14th.lte.statistic.entity.StatWeek;
import likelion14th.lte.todo.dto.request.TodoCreateRequest;
import likelion14th.lte.todo.dto.request.TodoUpdateRequest;
import likelion14th.lte.todo.entity.Todo;
import likelion14th.lte.todo.entity.TodoDate;
import likelion14th.lte.todo.entity.WeekEnum;
import likelion14th.lte.todo.generator.RoutineTodoDateGenerator;
import likelion14th.lte.todo.repository.TodoRepository;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import likelion14th.lte.youtube.domain.SavedSong;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.time.*;
import java.util.UUID;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoDatabasePersistenceTest {
    private static SessionFactory factory;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-11T15:30:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeAll
    static void isolatedDatabase() {
        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:isolated_persistence_audit;MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.setProperty("hibernate.connection.username", "sa");
        config.setProperty("hibernate.connection.password", "");
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        config.setProperty("hibernate.show_sql", "false");
        for (Class<?> entity : new Class<?>[]{User.class, Category.class, Todo.class, TodoDate.class,
                Statistic.class, StatWeek.class, SavedSong.class, RefreshToken.class, Follow.class}) {
            config.addAnnotatedClass(entity);
        }
        factory = config.buildSessionFactory();
        transaction(em -> { em.persist(BeanUtils.instantiateClass(Category.class)); return null; });
    }
    @AfterAll
    static void closeDatabase() { if (factory != null) factory.close(); }

    private static <T> T transaction(Function<EntityManager, T> work) {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            try {
                T result = work.apply(em);
                em.getTransaction().commit();
                return result;
            } catch (RuntimeException error) { em.getTransaction().rollback(); throw error; }
        }
    }
    private long user() {
        return transaction(em -> {
            User user = User.builder().username("isolated-test").userTag(UUID.randomUUID().toString().substring(0, 12)).build();
            em.persist(user); em.flush(); return user.getId();
        });
    }
    private TodoService service(EntityManager em) {
        JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
        TodoDateRepository dates = repos.getRepository(TodoDateRepository.class);
        return new TodoService(repos.getRepository(TodoRepository.class), dates, repos.getRepository(UserRepository.class),
                new RoutineTodoDateGenerator(dates) {}, repos.getRepository(CategoryRepository.class), CLOCK);
    }
    private TodoCreateRequest createRequest(boolean routine) {
        TodoCreateRequest request = mock(TodoCreateRequest.class);
        when(request.getDescription()).thenReturn("Persisted task");
        when(request.getCategoryName()).thenReturn("공부");
        when(request.isRoutineEnabled()).thenReturn(routine);
        when(request.getStartDate()).thenReturn(TODAY);
        when(request.getEndDate()).thenReturn(TODAY.plusDays(14));
        when(request.getWeek()).thenReturn(WeekEnum.THU);
        return request;
    }
    private TodoUpdateRequest updateRequest(boolean routine, String text) {
        TodoUpdateRequest request = mock(TodoUpdateRequest.class);
        when(request.getDescription()).thenReturn(text);
        when(request.getCategoryName()).thenReturn("공부");
        when(request.isRoutineEnabled()).thenReturn(routine);
        when(request.getStartDate()).thenReturn(TODAY);
        when(request.getEndDate()).thenReturn(TODAY.plusDays(14));
        when(request.getWeek()).thenReturn(WeekEnum.THU);
        return request;
    }

    private int count(long user, LocalDate day) {
        return transaction(em -> service(em).getTodosByDate(user, day).size());
    }

    @Test
    void committedCreateUpdateCompleteAndDeleteSurviveNewDatabaseSessions() {
        long user = user();
        long todo = transaction(em -> service(em).createTodo(user, createRequest(false), TODAY).getTodoId());
        assertEquals(1, count(user, TODAY));
        transaction(em -> service(em).updateTodoDetail(user, todo, updateRequest(false, "Updated task")));
        transaction(em -> service(em).todoComplete(user, todo, TODAY, true));
        transaction(em -> {
            var saved = service(em).getTodosByDate(user, TODAY).getFirst();
            assertEquals("Updated task", saved.getDescription());
            assertTrue(saved.isCompleted());
            return null;
        });
        transaction(em -> { service(em).deleteTodo(user, todo, TODAY); return null; });
        assertEquals(0, count(user, TODAY));
    }

    @Test
    void routineCompletionAndDeletedDateStayPreservedAcrossUpdatesAndScheduledGeneration() {
        long user = user();
        long todo = transaction(em -> service(em).createTodo(user, createRequest(true), TODAY).getTodoId());
        transaction(em -> service(em).todoComplete(user, todo, TODAY, true));
        transaction(em -> service(em).updateTodoDetail(user, todo, updateRequest(true, "Changed routine name")));
        boolean completed = transaction(em -> service(em).getTodosByDate(user, TODAY).getFirst().isCompleted());
        assertTrue(completed);
        LocalDate deletedDay = TODAY.plusDays(7);
        transaction(em -> { service(em).deleteTodo(user, todo, deletedDay); return null; });
        transaction(em -> {
            TodoDateRepository dates = new JpaRepositoryFactory(em).getRepository(TodoDateRepository.class);
            new RoutineTodoDateGenerator(dates) {}.generate(em.find(Todo.class, todo), TODAY, TODAY.plusDays(14), TODAY);
            return null;
        });
        assertEquals(0, count(user, deletedDay));
        assertEquals(1, count(user, TODAY.plusDays(14)));
        transaction(em -> {
            TodoDateRepository dates = new JpaRepositoryFactory(em).getRepository(TodoDateRepository.class);
            assertEquals(2, dates.summarizeByUserIdUntil(user, TODAY.plusDays(14)).size());
            assertEquals(3, dates.findAllByTodo_IdAndDateBetween(todo, TODAY, TODAY.plusDays(14)).size());
            return null;
        });
    }
    @Test
    void selectingAnOldSavedSongPersistsItsNewOrderWithoutDuplicateRows() {
        long user = user();
        long oldId = transaction(em -> {
            User owner = em.find(User.class, user);
            SavedSong old = SavedSong.builder().user(owner).songId("old-song").title("Old").artist("Artist").build();
            SavedSong newer = SavedSong.builder().user(owner).songId("new-song").title("New").artist("Artist").build();
            org.springframework.test.util.ReflectionTestUtils.setField(old, "savedAt", LocalDateTime.of(2020, 1, 1, 0, 0));
            org.springframework.test.util.ReflectionTestUtils.setField(newer, "savedAt", LocalDateTime.of(2021, 1, 1, 0, 0));
            em.persist(old); em.persist(newer); em.flush(); return old.getId();
        });
        transaction(em -> {
            JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
            var service = new likelion14th.lte.youtube.service.YouTubeService(
                    mock(likelion14th.lte.youtube.client.YouTubeClient.class),
                    repos.getRepository(likelion14th.lte.youtube.repository.SavedSongRepository.class),
                    repos.getRepository(UserRepository.class));
            service.saveSong(user, "old-song");
            return null;
        });
        transaction(em -> {
            var songs = new JpaRepositoryFactory(em).getRepository(likelion14th.lte.youtube.repository.SavedSongRepository.class)
                    .findAllByUserOrderBySavedAtDesc(em.find(User.class, user));
            assertEquals(2, songs.size());
            assertEquals(oldId, songs.getFirst().getId());
            return null;
        });
    }

    @Test
    void changingRoutineWeekKeepsCompletedHistoryAndRebuildsOnlyUnfinishedDates() {
        long user = user();
        long todo = transaction(em -> service(em).createTodo(user, createRequest(true), TODAY).getTodoId());
        transaction(em -> service(em).todoComplete(user, todo, TODAY, true));
        TodoUpdateRequest request = updateRequest(true, "Sunday routine");
        when(request.getWeek()).thenReturn(WeekEnum.SUN);
        transaction(em -> service(em).updateTodoDetail(user, todo, request));
        boolean completed = transaction(em -> service(em).getTodosByDate(user, TODAY).getFirst().isCompleted());
        assertTrue(completed);
        assertEquals(0, count(user, TODAY.plusDays(7)));
        assertEquals(1, count(user, LocalDate.of(2026, 11, 15)));
        assertEquals(1, count(user, LocalDate.of(2026, 11, 22)));
    }

}
