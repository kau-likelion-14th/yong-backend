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
                new RoutineTodoDateGenerator(dates, CLOCK) {}, repos.getRepository(CategoryRepository.class), CLOCK);
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
            new RoutineTodoDateGenerator(dates, CLOCK) {}.generate(em.find(Todo.class, todo), TODAY, TODAY.plusDays(14), TODAY);
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

    @Test
    void oldRoutineStartIncludesPastHistoryAndTheCurrentPeriod() {
        long user = user();
        TodoCreateRequest request = createRequest(true);
        when(request.getStartDate()).thenReturn(TODAY.minusYears(2));
        when(request.getEndDate()).thenReturn(TODAY.plusYears(3));
        transaction(em -> service(em).createTodo(user, request, TODAY));

        assertEquals(1, count(user, TODAY.minusYears(1).with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))));
        assertEquals(1, count(user, TODAY));
        assertEquals(1, count(user, TODAY.plusWeeks(51)));
        assertEquals(0, count(user, TODAY.plusYears(2)));
    }

    @Test
    void routineWithoutAMatchingWeekdayCannotBeCreatedOrUpdated() {
        long user = user();
        TodoCreateRequest invalid = createRequest(true);
        when(invalid.getEndDate()).thenReturn(TODAY);
        when(invalid.getWeek()).thenReturn(WeekEnum.FRI);
        var creationError = assertThrows(likelion14th.lte.global.exception.GeneralException.class,
                () -> transaction(em -> service(em).createTodo(user, invalid, TODAY)));
        assertEquals(likelion14th.lte.global.api.ErrorCode.TODO_ROUTINE_DAY_OF_WEEK_INVALID, creationError.getCode());
        assertEquals(0, count(user, TODAY));

        long todo = transaction(em -> service(em).createTodo(user, createRequest(true), TODAY).getTodoId());
        TodoUpdateRequest update = updateRequest(true, "Invalid weekday");
        when(update.getEndDate()).thenReturn(TODAY);
        when(update.getWeek()).thenReturn(WeekEnum.FRI);
        assertThrows(likelion14th.lte.global.exception.GeneralException.class,
                () -> transaction(em -> service(em).updateTodoDetail(user, todo, update)));
        transaction(em -> {
            Todo saved = em.find(Todo.class, todo);
            assertEquals(WeekEnum.THU, saved.getWeek());
            assertEquals("Persisted task", saved.getDescription());
            return null;
        });
        assertEquals(1, count(user, TODAY.plusDays(14)));
    }

    @Test
    void concurrentRoutineUpdatesLeaveDatesMatchingTheSavedSchedule() throws Exception {
        long user = user();
        long todo = transaction(em -> service(em).createTodo(user, createRequest(true), TODAY).getTodoId());
        TodoUpdateRequest sunday = updateRequest(true, "Sunday schedule");
        when(sunday.getWeek()).thenReturn(WeekEnum.SUN);
        TodoUpdateRequest monday = updateRequest(true, "Monday schedule");
        when(monday.getWeek()).thenReturn(WeekEnum.MON);
        var ready = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { ready.await(); return transaction(em -> service(em).updateTodoDetail(user, todo, sunday)); });
            var second = executor.submit(() -> { ready.await(); return transaction(em -> service(em).updateTodoDetail(user, todo, monday)); });
            ready.countDown();
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        transaction(em -> {
            Todo saved = em.find(Todo.class, todo);
            TodoDateRepository dates = new JpaRepositoryFactory(em).getRepository(TodoDateRepository.class);
            var occurrences = dates.findAllByTodo_IdAndDateBetween(todo, TODAY, TODAY.plusDays(14));
            assertFalse(occurrences.isEmpty());
            assertTrue(occurrences.stream().allMatch(date -> date.getDate().getDayOfWeek() == saved.getWeek().toDayOfWeek()));
            return null;
        });
    }

    @Test
    void withdrawalDeletesTodosDatesAndOtherUserRelations() {
        long user = user();
        long other = user();
        transaction(em -> service(em).createTodo(user, createRequest(true), TODAY));
        transaction(em -> {
            User owner = em.find(User.class, user);
            em.persist(Follow.builder().fromUser(em.find(User.class, other)).toUser(owner).build());
            em.persist(SavedSong.builder().user(owner).songId("withdraw-song").title("Song").artist("Artist").build());
            em.persist(RefreshToken.builder().user(owner).refreshToken("isolated-refresh").refreshTokenExpireTime(1L).build());
            return null;
        });
        transaction(em -> {
            JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
            new likelion14th.lte.login.service.AuthService(null, repos.getRepository(UserRepository.class),
                    null, null).withdraw(user);
            return null;
        });
        transaction(em -> {
            assertNull(em.find(User.class, user));
            assertNotNull(em.find(User.class, other));
            assertEquals(0L, em.createQuery("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :id", Long.class)
                    .setParameter("id", user).getSingleResult());
            assertEquals(0L, em.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.toUser.id = :id", Long.class)
                    .setParameter("id", user).getSingleResult());
            return null;
        });
    }

    @Test
    void legacyDuplicateFollowsCanBeListedAndRemoved() {
        long user = user();
        long other = user();
        transaction(em -> {
            for (int i = 0; i < 2; i++) {
                em.persist(Follow.builder().fromUser(em.find(User.class, user)).toUser(em.find(User.class, other)).build());
            }
            return null;
        });
        transaction(em -> {
            JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
            var follows = new likelion14th.lte.follow.service.FollowService(
                    repos.getRepository(likelion14th.lte.follow.repository.FollowRepository.class),
                    repos.getRepository(UserRepository.class)) {};
            assertEquals(1, follows.getFollowings(user).size());
            follows.unfollowUser(user, other);
            return null;
        });
        transaction(em -> {
            assertEquals(0L, em.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.fromUser.id = :id", Long.class)
                    .setParameter("id", user).getSingleResult());
            return null;
        });
    }

    @Test
    void legacyDuplicateSongsCanBeSelectedAndDeleted() {
        long user = user();
        transaction(em -> {
            for (int i = 0; i < 2; i++) {
                em.persist(SavedSong.builder().user(em.find(User.class, user))
                        .songId("duplicate-song").title("Song").artist("Artist").build());
            }
            return null;
        });
        transaction(em -> {
            JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
            var songs = new likelion14th.lte.youtube.service.YouTubeService(null,
                    repos.getRepository(likelion14th.lte.youtube.repository.SavedSongRepository.class),
                    repos.getRepository(UserRepository.class));
            assertEquals(1, songs.mySavedSongs(user).size());
            songs.saveSong(user, "duplicate-song");
            em.flush();
            assertEquals(1L, em.createQuery("SELECT COUNT(s) FROM SavedSong s WHERE s.user.id = :id", Long.class)
                    .setParameter("id", user).getSingleResult());
            songs.deleteSavedSong(user, "duplicate-song");
            return null;
        });
        transaction(em -> {
            assertEquals(0L, em.createQuery("SELECT COUNT(s) FROM SavedSong s WHERE s.user.id = :id", Long.class)
                    .setParameter("id", user).getSingleResult());
            return null;
        });
    }

}
