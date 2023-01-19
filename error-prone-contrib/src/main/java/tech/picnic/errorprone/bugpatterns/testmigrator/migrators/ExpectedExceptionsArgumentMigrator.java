package tech.picnic.errorprone.bugpatterns.testmigrator.migrators;

import static com.google.auto.common.MoreStreams.toImmutableList;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.NEW_ARRAY;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import java.util.Optional;
import tech.picnic.errorprone.bugpatterns.TestNGMetadata;
import tech.picnic.errorprone.bugpatterns.testmigrator.ArgumentMigrator;
import tech.picnic.errorprone.bugpatterns.testmigrator.TestNGMigrationContext;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

public class ExpectedExceptionsArgumentMigrator implements ArgumentMigrator {
  @Override
  public SuggestedFix createFix(
      TestNGMigrationContext context,
      MethodTree methodTree,
      ExpressionTree content,
      VisitorState state) {

    String expectedException = getExpectedException(content, state).orElseThrow();
    SuggestedFix.Builder fix =
        SuggestedFix.builder()
            .replace(
                methodTree.getBody(),
                buildWrappedBody(methodTree.getBody(), expectedException, state));

    ImmutableList<String> removedExceptions = getRemovedExceptions(content, state);
    if (!removedExceptions.isEmpty()) {
      fix.prefixWith(
          methodTree,
          String.format(
              "// XXX: Removed handling of `%s` because this migration doesn't support it.\n",
              String.join(", ", removedExceptions)));
    }

    return fix.build();
  }

  @Override
  public boolean canFix(
      TestNGMigrationContext context, TestNGMetadata.TestNGAnnotation annotation) {
    return annotation.getArgumentNames().contains("expectedExceptions");
  }

  private static Optional<String> getExpectedException(
      ExpressionTree expectedExceptions, VisitorState state) {
    if (expectedExceptions.getKind() == NEW_ARRAY) {
      NewArrayTree arrayTree = (NewArrayTree) expectedExceptions;
      if (arrayTree.getInitializers().isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(SourceCode.treeToString(arrayTree.getInitializers().get(0), state));
    } else if (expectedExceptions.getKind() == MEMBER_SELECT) {
      return Optional.of(SourceCode.treeToString(expectedExceptions, state));
    }

    return Optional.empty();
  }

  private static ImmutableList<String> getRemovedExceptions(
      ExpressionTree expectedExceptions, VisitorState state) {
    if (expectedExceptions.getKind() != NEW_ARRAY) {
      return ImmutableList.of();
    }

    NewArrayTree arrayTree = (NewArrayTree) expectedExceptions;
    if (arrayTree.getInitializers().size() <= 1) {
      return ImmutableList.of();
    }

    return arrayTree.getInitializers().subList(1, arrayTree.getInitializers().size()).stream()
        .map(initializer -> SourceCode.treeToString(initializer, state))
        .collect(toImmutableList());
  }

  private static String buildWrappedBody(BlockTree tree, String exception, VisitorState state) {
    return String.format(
        "{\norg.junit.jupiter.api.Assertions.assertThrows(%s, () -> %s);\n}",
        exception, SourceCode.treeToString(tree, state));
  }
}
