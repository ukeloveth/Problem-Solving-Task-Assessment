package com.test.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "tasks", indexes = {@Index(name ="idx_code",columnList = "code",unique = true),
        @Index(name = "idx_parent_code", columnList =  "parent_code")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false, length = 12)
    private String code;
    @Column(nullable = false)
    private String title;
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name="creator_id")
    private Long createdId;

    @Column(name="assigned_id")
    private Long assignedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_code",referencedColumnName = "code")
    @Setter
    private Task  parent;

    @OneToMany(mappedBy = "parent",cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<Task> children = new ArrayList<>();

    @Column(name = "priority")
    private String priority;

    @Column(name ="tags")
    private String tags;

    @Column(name="create_at")
    private LocalDateTime createAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    public int getHierarchyLevel(){
        int level = 1;
        Task current = this.parent;
        while(current != null && level < 5){
            level++;
            current = current.parent;
        }
        return level;
    }


    @PrePersist
    protected void onCreate(){
        createAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }


    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }





    





}
