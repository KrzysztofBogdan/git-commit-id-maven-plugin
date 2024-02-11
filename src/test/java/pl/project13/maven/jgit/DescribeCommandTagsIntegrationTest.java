/*
 * This file is part of git-commit-id-maven-plugin
 * Originally invented by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-maven-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-maven-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-maven-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.maven.jgit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;
import pl.project13.core.jgit.DescribeCommand;
import pl.project13.core.jgit.DescribeResult;
import pl.project13.log.DummyTestLoggerBridge;
import pl.project13.maven.git.AvailableGitTestRepo;
import pl.project13.maven.git.GitIntegrationTest;

/**
 * Testcases to verify that the {@link DescribeCommand} works properly.
 */
public class DescribeCommandTagsIntegrationTest extends GitIntegrationTest {

  static final String PROJECT_NAME = "my-jar-project";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();
  }

  @Override
  protected Optional<String> projectDir() {
    return Optional.of(PROJECT_NAME);
  }

  @Test
  public void shouldFindAnnotatedTagWithTagsOptionNotGiven() throws Exception {
    // given

    // HEAD
    // lightweight-tag
    // annotated-tag

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).contains("annotated-tag");
    }
  }

  @Test
  public void shouldFindLightweightTagWithTagsOptionGiven() throws Exception {
    // given

    // HEAD
    // lightweight-tag
    // annotated-tag

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).contains("lightweight-tag");
    }
  }

  @Test
  public void shouldFindAnnotatedTagWithMatchOptionGiven() throws Exception {
    // given

    // HEAD
    // lightweight-tag
    // annotated-tag

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge())
              .tags()
              .match("annotated*")
              .call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).contains("annotated-tag");
    }
  }

  /**
   *
   *
   * <pre>
   * $ lg
   *   * b6a73ed - (HEAD, master) third addition (32 hours ago) <p>Konrad Malawski</p>
   *   * d37a598 - (newest-tag, lightweight-tag) second line (32 hours ago) <p>Konrad Malawski</p>
   *   * 9597545 - (annotated-tag) initial commit (32 hours ago) <p>Konrad Malawski</p>
   *
   * $ git describe
   *   newest-tag-1-gb6a73ed
   * </pre>
   */
  @Test
  public void shouldFindNewerTagWhenACommitHasTwoOrMoreTags() throws Exception {
    // given

    // HEAD
    // lightweight-tag
    // annotated-tag

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      git.reset().setMode(ResetCommand.ResetType.HARD).call();

      // when
      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res).isNotNull();

      assertThat(res.toString()).isEqualTo("lightweight-tag-1-gb6a73ed");
    }
  }

  @Test
  public void shouldUseTheNewestTagOnACommitIfItHasMoreThanOneTags() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();

    String snapshotTag = "0.0.1-SNAPSHOT";
    String latestTag = "OName-0.0.1";

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      try (Git wrap = Git.wrap(repo)) {
        wrap.reset().setMode(ResetCommand.ResetType.HARD).call();

        // when
        wrap.tag().setName(snapshotTag).call();
        Thread.sleep(2000);
        wrap.tag().setName(latestTag).call();

        DescribeResult res =
            DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

        // then
        assertThat(res.toString()).isEqualTo(latestTag);
      }
    }
  }

  @Test
  public void shouldUseTheNewestTagOnACommitIfItHasMoreThanOneTagsReversedCase() throws Exception {
    // given
    mavenSandbox
        .withParentProject(PROJECT_NAME, "jar")
        .withNoChildProject()
        .withGitRepoInParent(AvailableGitTestRepo.WITH_LIGHTWEIGHT_TAG_BEFORE_ANNOTATED_TAG)
        .create();

    String beforeTag = "OName-0.0.1";
    String latestTag = "0.0.1-SNAPSHOT";

    try (final Git git = git();
        final Repository repo = git.getRepository()) {
      try (Git wrap = Git.wrap(repo)) {
        wrap.reset().setMode(ResetCommand.ResetType.HARD).call();

        // when
        wrap.tag().setName(beforeTag).call();
        wrap.tag().setName(latestTag).call();
      }

      DescribeResult res =
          DescribeCommand.on(evaluateOnCommit, repo, new DummyTestLoggerBridge()).tags().call();

      // then
      assertThat(res.toString()).isEqualTo(latestTag);
    }
  }
}
