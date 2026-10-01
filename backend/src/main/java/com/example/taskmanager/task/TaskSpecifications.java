package com.example.taskmanager.task;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

  private static final char LIKE_ESCAPE = '\\';

  private TaskSpecifications() {}

  public static Specification<Task> matching(TaskFilter filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.status() != null) {
        predicates.add(cb.equal(root.get("status"), filter.status()));
      }
      if (filter.priority() != null) {
        predicates.add(cb.equal(root.get("priority"), filter.priority()));
      }
      if (filter.unassigned()) {
        predicates.add(cb.isNull(root.get("assignee")));
      } else if (filter.assigneeId() != null) {
        predicates.add(cb.equal(root.get("assignee").get("id"), filter.assigneeId()));
      }
      if (filter.search() != null && !filter.search().isBlank()) {
        String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  static String escapeLike(String value) {
    return value
        .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }
}
