package likelion14th.lte.statistic.service;

import jakarta.persistence.EntityManager;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.statistic.dto.response.StatisticResponse;
import likelion14th.lte.statistic.entity.Statistic;
import likelion14th.lte.todo.repository.TodoDateRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StatisticService {
    private final UserRepository userRepository;
    private final TodoDateRepository todoDateRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public StatisticResponse getStatistic(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        return StatisticResponse.from(user.getStatistic());
    }

    @Transactional
    public void updateStatistic(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        LocalDate day = LocalDate.now().minusDays(1);
        LocalDate startDate = day.minusDays(30);

        boolean hasCompletedTodo = todoDateRepository.existsByTodo_User_IdAndDateAndCompleted(userId, day, true);
        boolean hasIncompleteTodo = todoDateRepository.existsByTodo_User_IdAndDateAndCompleted(userId, day, false);
        boolean success = hasCompletedTodo && !hasIncompleteTodo;

        Statistic statistic = user.getStatistic();
        statistic.increaseStreakIfSuccess(success);

        if (success) {
            statistic.increaseWeekCount(day);
        }

        long completedCount = todoDateRepository.countByTodo_User_IdAndDateBetweenAndCompleted(userId, startDate, day, true);
        long incompleteCount = todoDateRepository.countByTodo_User_IdAndDateBetweenAndCompleted(userId, startDate, day, false);
        statistic.updateMonthPercent(completedCount, incompleteCount);
    }

    @Transactional
    public void updateAllStatistics() {
        int page = 0;
        int size = 500;
        Page<User> users;

        do {
            users = userRepository.findAll(PageRequest.of(page, size));
            users.forEach(user -> updateStatistic(user.getId()));
            entityManager.flush();
            entityManager.clear();
            page++;
        } while (users.hasNext());
    }
}
