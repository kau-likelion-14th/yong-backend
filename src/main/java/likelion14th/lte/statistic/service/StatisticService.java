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

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class StatisticService {
    private final UserRepository userRepository;
    private final TodoDateRepository todoDateRepository;
    private final EntityManager entityManager;
    private final StatisticCalculator statisticCalculator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StatisticResponse getStatistic(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now(clock);
        StatisticSnapshot snapshot = calculateStatistic(userId, today);
        return StatisticResponse.from(snapshot);
    }

    private void updateStatistic(User user, LocalDate today) {
        StatisticSnapshot snapshot = calculateStatistic(user.getId(), today);
        Statistic statistic = user.getStatistic();
        statistic.replace(
                snapshot.streak(),
                snapshot.monthPercent(),
                snapshot.completedCountsByWeek()
        );
    }

    private StatisticSnapshot calculateStatistic(Long userId, LocalDate today) {
        return statisticCalculator.calculate(
                todoDateRepository.summarizeByUserIdUntil(userId, today),
                today
        );
    }

    @Transactional
    public void updateAllStatistics() {
        int page = 0;
        int size = 500;
        Page<User> users;
        LocalDate today = LocalDate.now(clock);

        do {
            users = userRepository.findAll(PageRequest.of(page, size));
            users.forEach(user -> updateStatistic(user, today));
            entityManager.flush();
            entityManager.clear();
            page++;
        } while (users.hasNext());
    }
}
