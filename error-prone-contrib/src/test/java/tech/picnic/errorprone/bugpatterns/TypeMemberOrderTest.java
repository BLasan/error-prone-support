package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class TypeMemberOrderTest {
  @Test
  void identification() {
    CompilationTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .expectErrorMessage(
            "TypeMemberOrder",
            message -> message.contains("Type members should be ordered in a standard way"))
        .addSourceLines(
            "A.java",
            "// BUG: Diagnostic matches: TypeMemberOrder",
            "class A {",
            "  char a = 'a';",
            "  private static String FOO = \"foo\";",
            "  static int ONE = 1;",
            "",
            "  void m2() {}",
            "",
            "  public A() {}",
            "",
            "  private static String BAR = \"bar\";",
            "  char b = 'b';",
            "",
            "  void m1() {",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  static int TWO = 2;",
            "",
            "  class Inner {}",
            "",
            "  static class StaticInner {}",
            "}")
        .addSourceLines(
            "B.java",
            "class B {",
            "  private static String FOO = \"foo\";",
            "  static int ONE = 1;",
            "  private static String BAR = \"bar\";",
            "",
            "  static int TWO = 2;",
            "",
            "  char a = 'a';",
            "",
            "  char b = 'b';",
            "",
            "  public B() {}",
            "",
            "  void m1() {",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void m2() {}",
            "",
            "  class Inner {}",
            "",
            "  static class StaticInner {}",
            "}")
        .addSourceLines(
            "C.java",
            "class C {",
            "  @SuppressWarnings({\"foo\", \"all\", \"bar\"})",
            "  void unorderedMethod() {}",
            "",
            "  C() {}",
            "}")
        .addSourceLines(
            "D.java",
            "class D {",
            "  @SuppressWarnings(\"TypeMemberOrder\")",
            "  void unorderedMethod() {}",
            "",
            "  D() {}",
            "}")
        .addSourceLines("Empty.java", "class Empty {}")
        .doTest();
  }

  @Test
  void replacementSuggestedFix() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "class A {",
            "  private static final int X = 1;",
            "  char a = 'a';",
            "",
            "  interface InnerInterface {}",
            "",
            "  class InnerClass {}",
            "",
            "  static class StaticInnerClass {}",
            "",
            "  private static String FOO = \"foo\";",
            "  static int ONE = 1;",
            "",
            "  void m2() {}",
            "",
            "  public A() {}",
            "",
            "  private static String BAR = \"bar\";",
            "  char b = 'b';",
            "",
            "  void m1() {",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  static int TWO = 2;",
            "",
            "  {",
            "    System.out.println(\"I'm an initializer block!\");",
            "  }",
            "",
            "  static {",
            "    System.out.println(\"I'm a static initializer block!\");",
            "  }",
            "}")
        .addOutputLines(
            "A.java",
            "class A {",
            "  private static final int X = 1;",
            "",
            "  private static String FOO = \"foo\";",
            "  static int ONE = 1;",
            "",
            "  private static String BAR = \"bar\";",
            "",
            "  static int TWO = 2;",
            "  char a = 'a';",
            "  char b = 'b';",
            "",
            "  static {",
            "    System.out.println(\"I'm a static initializer block!\");",
            "  }",
            "",
            "  {",
            "    System.out.println(\"I'm an initializer block!\");",
            "  }",
            "",
            "  public A() {}",
            "",
            "  void m2() {}",
            "",
            "  void m1() {",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  interface InnerInterface {}",
            "",
            "  class InnerClass {}",
            "",
            "  static class StaticInnerClass {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementSuggestedFix_Interface() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "interface A {",
            "",
            "  /* Detached comment */ ;",
            "",
            "  // comment",
            "  String staticField = \"string\";",
            "",
            "  private static void staticMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  default void defaultMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void member();",
            "",
            "  // another comment",
            "  String staticField2 = \"string\";",
            "",
            "  class innerClass {}",
            "",
            "  class innerStaticClass {}",
            "",
            "  interface innerInterface {}",
            "",
            "  // default member 2 comment",
            "  default void defaultMember2() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  // member 2 comment",
            "  void member2();",
            "",
            "  enum innerEnum {}",
            "}")
        .addOutputLines(
            "A.java",
            "interface A {",
            "",
            "  /* Detached comment */ ;",
            "",
            "  // comment",
            "  String staticField = \"string\";",
            "",
            "  // another comment",
            "  String staticField2 = \"string\";",
            "",
            "  private static void staticMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  default void defaultMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void member();",
            "",
            "  // default member 2 comment",
            "  default void defaultMember2() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  // member 2 comment",
            "  void member2();",
            "",
            "  class innerClass {}",
            "",
            "  class innerStaticClass {}",
            "",
            "  interface innerInterface {}",
            "",
            "  enum innerEnum {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementSuggestedFix_Enum() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "enum A {",
            "  /** Foo */",
            "  FOO,",
            "  /** BAR */",
            "  BAR,",
            "  /** BAZ */",
            "  BAZ",
            "/* Detached comment */ ;",
            "",
            "  private static void staticMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  // static field comment",
            "  static String staticField = \"string\";",
            "",
            "  void member() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void member2() {}",
            "",
            "  class innerClass {}",
            "",
            "  static class innerStaticClass {}",
            "",
            "  interface innerInterface {}",
            "",
            "  enum innerEnum {}",
            "}")
        .addOutputLines(
            "A.java",
            "enum A {",
            "  /** Foo */",
            "  FOO,",
            "  /** BAR */",
            "  BAR,",
            "  /** BAZ */",
            "  BAZ",
            "/* Detached comment */ ;",
            "",
            "  // static field comment",
            "  static String staticField = \"string\";",
            "",
            "  private static void staticMember() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void member() {",
            "    // comment",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  void member2() {}",
            "",
            "  class innerClass {}",
            "",
            "  static class innerStaticClass {}",
            "",
            "  interface innerInterface {}",
            "",
            "  enum innerEnum {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementSuggestedFixConsidersDefaultConstructor() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "class A {",
            "  void m1() {}",
            "",
            "  char c = 'c';",
            "",
            "  private static final String foo = \"foo\";",
            "",
            "  static int one = 1;",
            "}")
        .addOutputLines(
            "A.java",
            "class A {",
            "",
            "  private static final String foo = \"foo\";",
            "",
            "  static int one = 1;",
            "",
            "  char c = 'c';",
            "",
            "  void m1() {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @SuppressWarnings("ErrorProneTestHelperSourceFormat")
  @Test
  void replacementSuggestedFixConsidersComments() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "class A {",
            "  // detached comment from method",
            "  ;void method1() {}",
            "",
            "  // first comment prior to method",
            "  // second comment prior to method",
            "  void method2() {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "  // foo",
            "  /** Instantiates a new {@link A} instance. */",
            "  public A() {}",
            "}")
        .addOutputLines(
            "A.java",
            "class A {",
            "",
            "  // foo",
            "  /** Instantiates a new {@link A} instance. */",
            "  public A() {}",
            "  // detached comment from method",
            "  ;",
            "  void method1() {}",
            "",
            "  // first comment prior to method",
            "  // second comment prior to method",
            "  void method2() {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementSuggestedFixConsidersAnnotations() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "class A {",
            "  @SuppressWarnings(\"foo\")",
            "  void m1() {}",
            "",
            "  @SuppressWarnings(\"bar\")",
            "  A() {}",
            "}")
        .addOutputLines(
            "A.java",
            "class A {",
            "",
            "  @SuppressWarnings(\"bar\")",
            "  A() {}",
            "",
            "  @SuppressWarnings(\"foo\")",
            "  void m1() {}",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @SuppressWarnings("ErrorProneTestHelperSourceFormat")
  @Test
  void replacementSuggestedFixDoesNotModifyWhitespace() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "",
            "",
            "class A {",
            "",
            "",
            "  // `m1()` comment.",
            "  void m1() {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "  public  A  ()  {  }",
            "",
            "",
            "}")
        .addOutputLines(
            "A.java",
            "",
            "class A {",
            "  public  A  ()  {  }",
            "  // `m1()` comment.",
            "  void m1() {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  // XXX: This test should fail, if we verify that whitespace is preserved.
  @SuppressWarnings("ErrorProneTestHelperSourceFormat")
  void xxx() {
    BugCheckerRefactoringTestHelper.newInstance(TypeMemberOrder.class, getClass())
        .addInputLines(
            "A.java",
            "",
            "",
            "class A {",
            "",
            "",
            "  // `m1()` comment.",
            "  void m1() {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "  public  A  ()  {  }",
            "",
            "",
            "}")
        .addOutputLines(
            "A.java",
            "",
            "",
            "class A {",
            "",
            "  ",
            "     ",
            "  \t  \t",
            "     ",
            "  ",
            "",
            "  public  A                    ()  {  }",
            "  // `m1()` comment.",
            "  void m1",
            "         ()",
            "  {",
            "    // Print line 'foo' to stdout.",
            "    System.out.println(\"foo\");",
            "  }",
            "",
            "",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
