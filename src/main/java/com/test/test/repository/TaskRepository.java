package com.test.test.repository;

import com.test.test.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Task entity
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByCode(String code);

    Page<Task> findAll(Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.parent.code = :parentCode")
    List<Task> findByParentCode(@Param("parentCode") String parentCode);
    

    @Query("SELECT t FROM Task t WHERE t.parent IS NULL")
    List<Task> findRootTasks();
}