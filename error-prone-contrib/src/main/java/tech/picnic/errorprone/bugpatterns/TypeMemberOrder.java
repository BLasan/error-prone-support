package tech.picnic.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;

import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.util.Position;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import tech.picnic.errorprone.bugpatterns.util.SourceCode;

/**
 * A {@link BugChecker} that flags classes with a non-canonical member order.
 *
 * <p>Class members should be ordered as follows:
 *
 * <ol>
 *   <li>Static fields
 *   <li>Instance fields
 *   <li>Static initializer blocks
 *   <li>Instance initializer blocks
 *   <li>Constructors
 *   <li>Methods
 *   <li>Nested classes and interfaces
 * </ol>
 *
 * @see <a
 *     href="https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/DeclarationOrderCheck.html">Checkstyle's
 *     {@code DeclarationOrderCheck}</a>
 */
// XXX: Reference
// https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/DeclarationOrderCheck.html
@AutoService(BugChecker.class)
@BugPattern(
    summary = "Type members should be ordered in a standard way",
    link = BUG_PATTERNS_BASE_URL + "TypeMemberOrder",
    linkType = CUSTOM,
    severity = WARNING,
    tags = STYLE)
public final class TypeMemberOrder extends BugChecker implements BugChecker.ClassTreeMatcher {
  private static final long serialVersionUID = 1L;

  // TODO: Copy should be sorted and comparator in-sync.
  /** Orders {@link Tree}s to match the standard Java type member declaration order. */
  private static final Comparator<Tree> BY_PREFERRED_TYPE_MEMBER_ORDER =
      comparing(
          tree -> {
            switch (tree.getKind()) {
              case VARIABLE:
                return isStatic((VariableTree) tree) ? 1 : 2;
              case BLOCK:
                return isStatic((BlockTree) tree) ? 3 : 4;
              case METHOD:
                return isConstructor((MethodTree) tree) ? 5 : 6;
              case CLASS:
              case INTERFACE:
              case ENUM:
                return 7;
              default:
                throw new VerifyException("Unexpected member kind: " + tree.getKind());
            }
          });

  /**
   * Collection of values that are when provided in a {@link SuppressWarnings} annotation of a
   * member, this BugChecker will not sort it.
   */
  private static final ImmutableSet<String> RECOGNIZED_SUPPRESSIONS =
      ImmutableSet.of("all", TypeMemberOrder.class.getSimpleName());

  /** Instantiates a new {@link TypeMemberOrder} instance. */
  public TypeMemberOrder() {}

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    //    ImmutableList<TypeMember> typeMembers =
    //        getTypeMembersWithComments(tree, state).stream()
    //            .filter(typeMember -> canBeSorted(typeMember.tree()))
    //            .collect(toImmutableList());
    //
    //    ImmutableList<TypeMember> sortedTypeMembers =
    //        ImmutableList.sortedCopyOf(
    //            comparing(TypeMember::tree, BY_PREFERRED_TYPE_MEMBER_ORDER), typeMembers);
    //
    //    if (typeMembers.equals(sortedTypeMembers)) {
    //      return Description.NO_MATCH;
    //    }
    //
    //    return buildDescription(tree)
    //        .addFix(replaceTypeMembers(typeMembers, sortedTypeMembers, state))
    //        .build();
    ImmutableList<Tree> typeMemberTrees =
        tree.getMembers().stream().filter(TypeMemberOrder::canBeSorted).collect(toImmutableList());

    ImmutableList<Tree> sortedTypeMemberTrees =
        ImmutableList.sortedCopyOf(BY_PREFERRED_TYPE_MEMBER_ORDER, typeMemberTrees);

    if (typeMemberTrees.equals(sortedTypeMemberTrees)) {
      return Description.NO_MATCH;
    }

    ImmutableList<TypeMember> sortedTypeMembers =
        sortedTypeMemberTrees.stream()
            .map(member -> getTypeMembersWithComments(tree, member, state))
            .collect(toImmutableList());

    SuggestedFix fix = replaceTypeMembers(typeMemberTrees, sortedTypeMembers, state);

    return buildDescription(tree).addFix(fix).build();
  }

  private static boolean isStatic(VariableTree variableTree) {
    Set<Modifier> modifiers = variableTree.getModifiers().getFlags();
    return modifiers.contains(Modifier.STATIC);
  }

  private static boolean isStatic(BlockTree blockTree) {
    return blockTree.isStatic();
  }

  private static boolean isConstructor(MethodTree methodTree) {
    return ASTHelpers.getSymbol(methodTree).isConstructor();
  }

  private static boolean canBeSorted(Tree tree) {
    if (hasRecognizedSuppressWarnings(tree)) {
      return false;
    }
    return tree instanceof VariableTree
        || (tree instanceof MethodTree && !ASTHelpers.isGeneratedConstructor((MethodTree) tree))
        || tree instanceof BlockTree
        || tree instanceof ClassTree;
  }

  private static Boolean hasRecognizedSuppressWarnings(Tree tree) {
    return Optional.ofNullable(
            ASTHelpers.getAnnotationWithSimpleName(
                ASTHelpers.getAnnotations(tree), "SuppressWarnings"))
        .flatMap(
            suppressWarningsTree ->
                ASTHelpers.getAnnotationMirror(suppressWarningsTree)
                    .getElementValues()
                    .values()
                    .stream()
                    // Assuming SuppressWarnings has a single member (`String[] value()`)
                    .findAny())
        .map(
            annotationValue ->
                ((Attribute.Array) annotationValue)
                    .getValue().map(attr -> (String) attr.getValue()).stream()
                        .anyMatch(RECOGNIZED_SUPPRESSIONS::contains))
        .orElse(false);
  }

  private static SuggestedFix replaceTypeMembers(
      ImmutableList<Tree> typeMembers,
      ImmutableList<TypeMember> replacementTypeMembers,
      VisitorState state) {
    return Streams.zip(
            typeMembers.stream(),
            replacementTypeMembers.stream(),
            (original, replacement) -> replaceTypeMember(original, replacement, state))
        .reduce(SuggestedFix.builder(), SuggestedFix.Builder::merge, SuggestedFix.Builder::merge)
        .build();
  }

  private static SuggestedFix replaceTypeMember(
      Tree original, TypeMember replacement, VisitorState state) {
    /* Technically this check is not necessary, but it avoids redundant replacements. */
    if (original.equals(replacement.tree())) {
      return SuggestedFix.emptyFix();
    }

    String replacementSource =
        Stream.concat(
                replacement.comments().stream(),
                Stream.of(SourceCode.treeToString(replacement.tree(), state)))
            .collect(joining(System.lineSeparator()));
    return SuggestedFixes.replaceIncludingComments(
        TreePath.getPath(state.getPath(), original), replacementSource, state);
  }

  /** Returns the type's members with their comments. */
  private static TypeMember getTypeMembersWithComments(
      ClassTree tree, Tree member, VisitorState state) {
    return new AutoValue_TypeMemberOrder_TypeMember(
        member, getTypeMemberComments(tree, member, state));
  }

  private static ImmutableList<String> getTypeMemberComments(
      ClassTree tree, Tree member, VisitorState state) {
    int typeStart = ASTHelpers.getStartPosition(tree);
    int typeEnd = state.getEndPosition(tree);
    int memberStart = ASTHelpers.getStartPosition(member);
    int memberEnd = state.getEndPosition(member);
    if (typeStart == Position.NOPOS
        || typeEnd == Position.NOPOS
        || memberStart == Position.NOPOS
        || memberEnd == Position.NOPOS) {
      /* Source code details appear to be unavailable. */
      return ImmutableList.of();
    }

    // TODO: Move identifying "previous member end position" to an outer loop,
    //  Loop once and identify for all members
    // TODO: Check if this handles properly comments on the first member.
    Optional<Integer> previousMemberEndPos =
        state.getOffsetTokens(typeStart, typeEnd).stream()
            .map(ErrorProneToken::endPos)
            .takeWhile(endPos -> endPos < memberStart)
            .reduce((earlierPos, laterPos) -> laterPos);

    List<ErrorProneToken> typeMemberTokens =
        state.getOffsetTokens(previousMemberEndPos.orElse(memberStart), memberEnd);

    // TODO: double check this .get(0)
    return typeMemberTokens.get(0).comments().stream()
        .map(Tokens.Comment::getText)
        .collect(toImmutableList());
  }

  @AutoValue
  abstract static class TypeMember {
    abstract Tree tree();

    abstract ImmutableList<String> comments();
  }
}
