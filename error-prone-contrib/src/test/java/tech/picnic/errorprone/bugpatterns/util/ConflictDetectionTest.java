package tech.picnic.errorprone.bugpatterns.util;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import org.junit.jupiter.api.Test;

final class ConflictDetectionTest {
  @Test
  void matcher() {
    CompilationTestHelper.newInstance(RenameBlockerFlagger.class, getClass())
        .addSourceLines(
            "/A.java",
            "import static staticimport.B.foo3t;",
            "",
            "class A {",
            "  private void foo1() {",
            "    foo3t();",
            "  }",
            "",
            "  // BUG: Diagnostic contains: [RenameBlockerFlagger] a method named `foo2t` already exists in this",
            "  // class",
            "  private void foo2() {}",
            "",
            "  private void foo2t() {}",
            "",
            "  // BUG: Diagnostic contains: [RenameBlockerFlagger] `foo3t` is already statically imported",
            "  private void foo3() {}",
            "",
            "  // BUG: Diagnostic contains: [RenameBlockerFlagger] `int` is a reserved keyword",
            "  private void in() {}",
            "}")
        .addSourceLines(
            "/staticimport/B.java",
            "package staticimport;",
            "",
            "public class B {",
            "  public static void foo3t() {}",
            "}")
        .doTest();
  }

  @BugPattern(summary = "Flags blockers for renaming methods", severity = ERROR)
  public static final class RenameBlockerFlagger extends BugChecker
      implements BugChecker.MethodTreeMatcher {
    private static final long serialVersionUID = 1L;

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return ConflictDetection.findMethodRenameBlocker(tree.getName() + "t", state)
          .map(blocker -> buildDescription(tree).setMessage(blocker).build())
          .orElse(Description.NO_MATCH);
    }
  }
}
