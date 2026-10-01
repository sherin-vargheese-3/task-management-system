package com.example.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskSpecificationsTest {

  @Test
  void escapeLikeEscapesWildcardsAndEscapeCharacter() {
    String escaped = TaskSpecifications.escapeLike("100%_a\\b");

    assertThat(escaped).isEqualTo("100\\%\\_a\\\\b");
  }
}
