package tech.picnic.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.StandardTags.LIKELY_ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static tech.picnic.errorprone.bugpatterns.util.Documentation.BUG_PATTERNS_BASE_URL;
import static tech.picnic.errorprone.bugpatterns.util.MoreTypes.generic;
import static tech.picnic.errorprone.bugpatterns.util.MoreTypes.type;
import static tech.picnic.errorprone.bugpatterns.util.MoreTypes.unbound;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;
import reactor.core.publisher.Mono;

/**
 * A {@link BugChecker} that flags usages of {@link Mono#zip(Mono, Mono)}} and {@link
 * Mono#zipWith(Mono)}} with {@link Mono#empty()} parameters.
 *
 * <p>{@link Mono#zip(Mono, Mono)} and {@link Mono#zipWith(Mono)} perform incorrectly upon retrieval
 * of the empty publisher and prematurely terminates the reactive chain from the execution. In most
 * cases this is not the desired behaviour.
 *
 * <p>NB: Mono&lt;?>#zipWith(Mono&lt;Void>) is allowed be the Reactor API, but it is an incorrect
 * usage of the API. It will be flagged by ErrorProne but the fix won't be supplied. The problem
 * with the original code should be revisited and fixed in a structural manner by the developer.
 */
@AutoService(BugChecker.class)
@BugPattern(
    summary =
        "`Mono#zip` and `Mono#zipWith` should not be executed against `Mono#empty` or `Mono<Void>` parameter; "
            + "please revisit the parameters used and make sure to supply correct publishers instead",
    link = BUG_PATTERNS_BASE_URL + "MonoZipOfMonoVoidUsage",
    linkType = CUSTOM,
    severity = ERROR,
    tags = LIKELY_ERROR)
public final class MonoZipOfMonoVoidUsage extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {
  private static final long serialVersionUID = 1L;
  private static final Supplier<Type> MONO = type(Mono.class.getName());
  // Mono.empty() yields `Mono<Object>` under the hood
  private static final Supplier<Type> MONO_OBJECT_TYPE =
      VisitorState.memoize(generic(MONO, type(Object.class.getName())));
  // In fact, we use `Mono<Void>` everywhere in codebases instead of `Mono<Object>` to represent
  // empty publisher
  private static final Supplier<Type> MONO_VOID_TYPE =
      VisitorState.memoize(generic(MONO, type(Void.class.getName())));

  private static final Supplier<Type> MONO_UNBOUND_TYPE =
      VisitorState.memoize(generic(MONO, unbound()));

  // On Mono.zip, at least one element should match empty in order to proceed.
  private static final Matcher<ExpressionTree> MONO_ZIP_AND_WITH =
      anyOf(
          allOf(
              instanceMethod().onDescendantOf(MONO_UNBOUND_TYPE).namedAnyOf("zip", "zipWith"),
              toType(
                  MethodInvocationTree.class,
                  hasArgumentOfTypes(ImmutableList.of(MONO_VOID_TYPE, MONO_OBJECT_TYPE)))),
          allOf(
              instanceMethod().onDescendantOf(MONO_VOID_TYPE).namedAnyOf("zip", "zipWith"),
              toType(MethodInvocationTree.class, hasArgumentOfType(MONO_UNBOUND_TYPE))),
          allOf(
              instanceMethod().onDescendantOf(MONO_OBJECT_TYPE).namedAnyOf("zip", "zipWith"),
              toType(MethodInvocationTree.class, hasArgumentOfType(MONO_UNBOUND_TYPE))));

  // On Mono.zip, at least one element should match empty in order to proceed.
  private static final Matcher<ExpressionTree> STATIC_MONO_ZIP =
      allOf(
          staticMethod().onClass(MONO).named("zip"),
          toType(
              MethodInvocationTree.class,
              hasArgumentOfTypes(ImmutableList.of(MONO_OBJECT_TYPE, MONO_VOID_TYPE))));

  /** Instantiates a new {@link MonoZipOfMonoVoidUsage} instance. */
  public MonoZipOfMonoVoidUsage() {}

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    boolean dynamicMono = MONO_ZIP_AND_WITH.matches(tree, state);
    boolean staticMono = STATIC_MONO_ZIP.matches(tree, state);
    if (!dynamicMono && !staticMono) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            "`Mono#zip` and `Mono#zipWith` should not be executed against empty publisher; "
                + "remove it or suppress this warning and add a comment explaining its purpose")
        .addFix(SuggestedFixes.addSuppressWarnings(state, canonicalName()))
        .build();
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    boolean dynamicMono = MONO_ZIP_AND_WITH.matches(tree, state);
    boolean staticMono = STATIC_MONO_ZIP.matches(tree, state);

    if (!dynamicMono && !staticMono) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static Matcher<MethodInvocationTree> hasArgumentOfType(Supplier<Type> type) {
    return hasArgumentOfTypes(ImmutableList.of(type));
  }

  /**
   * We need to extract real types from the generics because {@link ASTHelpers} cannot distinguish
   * {@link Mono}&lt;{@link Integer}&gt; and {@link Mono}&lt;{@link Void}&gt; and reports those
   * being the same.
   *
   * <p>In case of {@link Mono}, we can infer the real type out of the parameters of the invocation
   * ({@link MethodInvocationTree#getArguments()}):
   *
   * <p>- either we have explicit variable declared and the provided type which will be inferred,
   *
   * <p>- or we have a method invocation, like {@link Mono#just(Object)} or {@link Mono#empty()},
   * for which we can also infer type.
   *
   * <p>Similarly, we can infer the matching type
   *
   * <p>In this case we will always have only one parameter.
   */
  private static Matcher<MethodInvocationTree> hasArgumentOfTypes(
      ImmutableList<Supplier<Type>> types) {
    return (tree, state) ->
        tree.getArguments().stream()
            .anyMatch(
                arg -> {
                  List<Type> allParams = ASTHelpers.getType(arg).allparams();
                  if (allParams.isEmpty()) {
                    return false;
                  }

                  Type argumentType = allParams.get(0);
                  return types.stream()
                      .map(type -> type.get(state).allparams().get(0))
                      .anyMatch(
                          requiredMatchingType ->
                              isSameType(argumentType, requiredMatchingType, state));
                });
  }
}
