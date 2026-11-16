package likelion14th.lte.follow.repository;

import likelion14th.lte.follow.entity.Follow;
import likelion14th.lte.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFromUserAndToUser(User fromUser, User toUser);

    List<Follow> findAllByFromUserAndToUser(User fromUser, User toUser);

    List<Follow> findByToUser(User toUser);

    List<Follow> findByFromUser(User fromUser);
}
