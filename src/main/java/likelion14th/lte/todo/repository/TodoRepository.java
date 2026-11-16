package likelion14th.lte.todo.repository;

import likelion14th.lte.todo.entity.Todo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Todo t WHERE t.id = :todoId")
    Optional<Todo> findByIdForUpdate(@Param("todoId") Long todoId);

    //스캐줄러에서 루틴인 애들을 전부 싸그리 긁어다가 투두데이트 애들 쭉생성해줄려고
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Todo t WHERE t.routineEnabled = true ORDER BY t.id")
    List<Todo> findAllByRoutineEnabledTrue();
}
