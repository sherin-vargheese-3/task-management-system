package com.example.taskmanager.task;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  @Override
  @EntityGraph(attributePaths = "assignee")
  Page<Task> findAll(Specification<Task> spec, Pageable pageable);

  @Query("select t.status as status, count(t) as count from Task t group by t.status")
  List<StatusCount> countByStatus();

  interface StatusCount {
    TaskStatus getStatus();

    long getCount();
  }
}
