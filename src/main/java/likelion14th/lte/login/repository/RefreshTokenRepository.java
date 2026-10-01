package likelion14th.lte.login.repository;


import likelion14th.lte.login.domain.RefreshToken;
import likelion14th.lte.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken refreshToken where refreshToken.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
