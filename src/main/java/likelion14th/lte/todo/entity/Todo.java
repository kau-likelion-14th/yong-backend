package likelion14th.lte.todo.entity;


import jakarta.persistence.*;
import likelion14th.lte.Entity.BaseEntity;
import likelion14th.lte.category.entity.Category;
import likelion14th.lte.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "todo")
public class Todo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean routineEnabled;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "todo",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoDate> todoDates = new ArrayList<>();

   @Enumerated(EnumType.STRING)
   private WeekEnum week;

    public static Todo create(User user, String description,boolean routineEnabled,
                              LocalDate startDate, LocalDate endDate, Category category, WeekEnum week)
    {
        Todo todo = new Todo();
        todo.user = user;
        todo.description = description;
        todo.routineEnabled = routineEnabled;
        todo.startDate = startDate;
        todo.endDate = endDate;
        todo.category = category;
        todo.week = week;
        return todo;
    }

   public void update (String description, Category category,
                       WeekEnum week ,boolean routineEnabled, LocalDate startDate, LocalDate endDate){

       this.description = description;
       this.category = category;
       this.week = week;
       this.routineEnabled = routineEnabled;
       this.startDate = startDate;
       this.endDate = endDate;
   }
}
