package tech.picnic.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

final class IdentityConversionTest {
  @Test
  void identification() {
    CompilationTestHelper.newInstance(IdentityConversion.class, getClass())
        .addSourceLines(
            "A.java",
            """
            import static com.google.errorprone.matchers.Matchers.instanceMethod;
            import static com.google.errorprone.matchers.Matchers.staticMethod;

            import com.google.common.collect.ImmutableBiMap;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableListMultimap;
            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableMultimap;
            import com.google.common.collect.ImmutableMultiset;
            import com.google.common.collect.ImmutableRangeMap;
            import com.google.common.collect.ImmutableRangeSet;
            import com.google.common.collect.ImmutableSet;
            import com.google.common.collect.ImmutableSetMultimap;
            import com.google.common.collect.ImmutableTable;
            import com.google.errorprone.matchers.Matcher;
            import com.google.errorprone.matchers.Matchers;
            import reactor.adapter.rxjava.RxJava2Adapter;
            import reactor.core.publisher.Flux;
            import reactor.core.publisher.Mono;

            public final class A {
              public void m() {
                // BUG: Diagnostic contains:
                Boolean b1 = Boolean.valueOf(Boolean.FALSE);
                // BUG: Diagnostic contains:
                Boolean b2 = Boolean.valueOf(false);
                // BUG: Diagnostic contains:
                boolean b3 = Boolean.valueOf(Boolean.FALSE);
                // BUG: Diagnostic contains:
                boolean b4 = Boolean.valueOf(false);

                // BUG: Diagnostic contains:
                Byte byte1 = Byte.valueOf((Byte) Byte.MIN_VALUE);
                // BUG: Diagnostic contains:
                Byte byte2 = Byte.valueOf(Byte.MIN_VALUE);
                // BUG: Diagnostic contains:
                byte byte3 = Byte.valueOf((Byte) Byte.MIN_VALUE);
                // BUG: Diagnostic contains:
                byte byte4 = Byte.valueOf(Byte.MIN_VALUE);

                // BUG: Diagnostic contains:
                Character c1 = Character.valueOf((Character) 'a');
                // BUG: Diagnostic contains:
                Character c2 = Character.valueOf('a');
                // BUG: Diagnostic contains:
                char c3 = Character.valueOf((Character) 'a');
                // BUG: Diagnostic contains:
                char c4 = Character.valueOf('a');

                // BUG: Diagnostic contains:
                Double d1 = Double.valueOf((Double) 0.0);
                // BUG: Diagnostic contains:
                Double d2 = Double.valueOf(0.0);
                // BUG: Diagnostic contains:
                double d3 = Double.valueOf((Double) 0.0);
                // BUG: Diagnostic contains:
                double d4 = Double.valueOf(0.0);

                // BUG: Diagnostic contains:
                Float f1 = Float.valueOf((Float) 0.0F);
                // BUG: Diagnostic contains:
                Float f2 = Float.valueOf(0.0F);
                // BUG: Diagnostic contains:
                float f3 = Float.valueOf((Float) 0.0F);
                // BUG: Diagnostic contains:
                float f4 = Float.valueOf(0.0F);

                // BUG: Diagnostic contains:
                Integer i1 = Integer.valueOf((Integer) 1);
                // BUG: Diagnostic contains:
                Integer i2 = Integer.valueOf(1);
                // BUG: Diagnostic contains:
                int i3 = Integer.valueOf((Integer) 1);
                // BUG: Diagnostic contains:
                int i4 = Integer.valueOf(1);

                // BUG: Diagnostic contains:
                Long l1 = Long.valueOf((Long) 1L);
                // BUG: Diagnostic contains:
                Long l2 = Long.valueOf(1L);
                // BUG: Diagnostic contains:
                long l3 = Long.valueOf((Long) 1L);
                // BUG: Diagnostic contains:
                long l4 = Long.valueOf(1L);

                Long l5 = Long.valueOf((Integer) 1);
                Long l6 = Long.valueOf(1);
                // BUG: Diagnostic contains:
                long l7 = Long.valueOf((Integer) 1);
                // BUG: Diagnostic contains:
                long l8 = Long.valueOf(1);

                // BUG: Diagnostic contains:
                Short s1 = Short.valueOf((Short) Short.MIN_VALUE);
                // BUG: Diagnostic contains:
                Short s2 = Short.valueOf(Short.MIN_VALUE);
                // BUG: Diagnostic contains:
                short s3 = Short.valueOf((Short) Short.MIN_VALUE);
                // BUG: Diagnostic contains:
                short s4 = Short.valueOf(Short.MIN_VALUE);

                // BUG: Diagnostic contains:
                String boolStr = Boolean.valueOf(Boolean.FALSE).toString();
                int boolHash = Boolean.valueOf(false).hashCode();
                // BUG: Diagnostic contains:
                int byteHash = Byte.valueOf((Byte) Byte.MIN_VALUE).hashCode();
                String byteStr = Byte.valueOf(Byte.MIN_VALUE).toString();

                String str1 = String.valueOf(0);
                // BUG: Diagnostic contains:
                String str2 = String.valueOf("1");

                // BUG: Diagnostic contains:
                ImmutableBiMap<Object, Object> o1 = ImmutableBiMap.copyOf(ImmutableBiMap.of());
                // BUG: Diagnostic contains:
                ImmutableList<Object> o2 = ImmutableList.copyOf(ImmutableList.of());
                ImmutableListMultimap<Object, Object> o3 =
                    // BUG: Diagnostic contains:
                    ImmutableListMultimap.copyOf(ImmutableListMultimap.of());
                // BUG: Diagnostic contains:
                ImmutableMap<Object, Object> o4 = ImmutableMap.copyOf(ImmutableMap.of());
                // BUG: Diagnostic contains:
                ImmutableMultimap<Object, Object> o5 = ImmutableMultimap.copyOf(ImmutableMultimap.of());
                // BUG: Diagnostic contains:
                ImmutableMultiset<Object> o6 = ImmutableMultiset.copyOf(ImmutableMultiset.of());
                // BUG: Diagnostic contains:
                ImmutableRangeMap<String, Object> o7 = ImmutableRangeMap.copyOf(ImmutableRangeMap.of());
                // BUG: Diagnostic contains:
                ImmutableRangeSet<String> o8 = ImmutableRangeSet.copyOf(ImmutableRangeSet.of());
                // BUG: Diagnostic contains:
                ImmutableSet<Object> o9 = ImmutableSet.copyOf(ImmutableSet.of());
                ImmutableSetMultimap<Object, Object> o10 =
                    // BUG: Diagnostic contains:
                    ImmutableSetMultimap.copyOf(ImmutableSetMultimap.of());
                // BUG: Diagnostic contains:
                ImmutableTable<Object, Object, Object> o11 = ImmutableTable.copyOf(ImmutableTable.of());

                // BUG: Diagnostic contains:
                Matcher allOf1 = Matchers.allOf(instanceMethod());
                Matcher allOf2 = Matchers.allOf(instanceMethod(), staticMethod());
                // BUG: Diagnostic contains:
                Matcher anyOf1 = Matchers.anyOf(staticMethod());
                Matcher anyOf2 = Matchers.anyOf(instanceMethod(), staticMethod());

                // BUG: Diagnostic contains:
                Flux<Integer> flux1 = Flux.just(1).flatMap(e -> RxJava2Adapter.fluxToFlowable(Flux.just(2)));

                // BUG: Diagnostic contains:
                Flux<Integer> flux2 = Flux.concat(Flux.just(1));
                // BUG: Diagnostic contains:
                Flux<Integer> flux3 = Flux.firstWithSignal(Flux.just(1));
                // BUG: Diagnostic contains:
                Flux<Integer> flux4 = Flux.from(Flux.just(1));
                // BUG: Diagnostic contains:
                Flux<Integer> flux5 = Flux.merge(Flux.just(1));

                // BUG: Diagnostic contains:
                Mono<Integer> mono1 = Mono.from(Mono.just(1));
                // BUG: Diagnostic contains:
                Mono<Integer> mono2 = Mono.fromDirect(Mono.just(1));
              }
            }
            """)
        .doTest();
  }

  @Test
  void replacementFirstSuggestedFix() {
    BugCheckerRefactoringTestHelper.newInstance(IdentityConversion.class, getClass())
        .addInputLines(
            "A.java",
            """
            import static com.google.errorprone.matchers.Matchers.staticMethod;
            import static org.mockito.Mockito.when;

            import com.google.common.collect.ImmutableCollection;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import com.google.errorprone.matchers.Matcher;
            import com.google.errorprone.matchers.Matchers;
            import java.util.ArrayList;
            import java.util.Collection;
            import org.reactivestreams.Publisher;
            import reactor.adapter.rxjava.RxJava2Adapter;
            import reactor.core.publisher.Flux;
            import reactor.core.publisher.Mono;

            public final class A {
              public void m() {
                ImmutableSet<Object> set1 = ImmutableSet.copyOf(ImmutableSet.of());
                ImmutableSet<Object> set2 = ImmutableSet.copyOf(ImmutableList.of());

                ImmutableCollection<Integer> list1 = ImmutableList.copyOf(ImmutableList.of(1));
                ImmutableCollection<Integer> list2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));

                Collection<Integer> c1 = ImmutableSet.copyOf(ImmutableSet.of(1));
                Collection<Integer> c2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));

                Flux<Integer> f1 = Flux.just(1).flatMap(e -> RxJava2Adapter.fluxToFlowable(Flux.just(2)));
                Flux<Integer> f2 = Flux.concat(Flux.just(3));
                Publisher<Integer> f3 = Flux.firstWithSignal(Flux.just(4));
                Publisher<Integer> f4 = Flux.from(Flux.just(5));
                Publisher<Integer> f5 = Flux.merge(Flux.just(6));

                Mono<Integer> m1 = Mono.from(Mono.just(7));
                Publisher<Integer> m2 = Mono.fromDirect(Mono.just(8));

                bar(Flux.concat(Flux.just(9)));
                bar(Mono.from(Mono.just(10)));

                Object o1 = ImmutableSet.copyOf(ImmutableList.of());
                Object o2 = ImmutableSet.copyOf(ImmutableSet.of());

                Matcher matcher = Matchers.allOf(staticMethod());

                when("foo".contains("f")).thenAnswer(inv -> ImmutableSet.copyOf(ImmutableList.of(1)));
              }

              void bar(Publisher<Integer> publisher) {}
            }
            """)
        .addOutputLines(
            "A.java",
            """
            import static com.google.errorprone.matchers.Matchers.staticMethod;
            import static org.mockito.Mockito.when;

            import com.google.common.collect.ImmutableCollection;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import com.google.errorprone.matchers.Matcher;
            import com.google.errorprone.matchers.Matchers;
            import java.util.ArrayList;
            import java.util.Collection;
            import org.reactivestreams.Publisher;
            import reactor.adapter.rxjava.RxJava2Adapter;
            import reactor.core.publisher.Flux;
            import reactor.core.publisher.Mono;

            public final class A {
              public void m() {
                ImmutableSet<Object> set1 = ImmutableSet.of();
                ImmutableSet<Object> set2 = ImmutableSet.copyOf(ImmutableList.of());

                ImmutableCollection<Integer> list1 = ImmutableList.of(1);
                ImmutableCollection<Integer> list2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));

                Collection<Integer> c1 = ImmutableSet.of(1);
                Collection<Integer> c2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));

                Flux<Integer> f1 = Flux.just(1).flatMap(e -> Flux.just(2));
                Flux<Integer> f2 = Flux.just(3);
                Publisher<Integer> f3 = Flux.just(4);
                Publisher<Integer> f4 = Flux.just(5);
                Publisher<Integer> f5 = Flux.just(6);

                Mono<Integer> m1 = Mono.just(7);
                Publisher<Integer> m2 = Mono.just(8);

                bar(Flux.just(9));
                bar(Mono.just(10));

                Object o1 = ImmutableSet.copyOf(ImmutableList.of());
                Object o2 = ImmutableSet.of();

                Matcher matcher = staticMethod();

                when("foo".contains("f")).thenAnswer(inv -> ImmutableSet.copyOf(ImmutableList.of(1)));
              }

              void bar(Publisher<Integer> publisher) {}
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  void replacementSecondSuggestedFix() {
    BugCheckerRefactoringTestHelper.newInstance(IdentityConversion.class, getClass())
        .setFixChooser(FixChoosers.SECOND)
        .addInputLines(
            "A.java",
            """
            import com.google.common.collect.ImmutableCollection;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import java.util.ArrayList;

            public final class A {
              public void m() {
                ImmutableSet<Object> set1 = ImmutableSet.copyOf(ImmutableSet.of());
                ImmutableSet<Object> set2 = ImmutableSet.copyOf(ImmutableList.of());

                ImmutableCollection<Integer> list1 = ImmutableList.copyOf(ImmutableList.of(1));
                ImmutableCollection<Integer> list2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));
              }
            }
            """)
        .addOutputLines(
            "A.java",
            """
            import com.google.common.collect.ImmutableCollection;
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import java.util.ArrayList;

            public final class A {
              public void m() {
                @SuppressWarnings("IdentityConversion")
                ImmutableSet<Object> set1 = ImmutableSet.copyOf(ImmutableSet.of());
                ImmutableSet<Object> set2 = ImmutableSet.copyOf(ImmutableList.of());

                @SuppressWarnings("IdentityConversion")
                ImmutableCollection<Integer> list1 = ImmutableList.copyOf(ImmutableList.of(1));
                ImmutableCollection<Integer> list2 = ImmutableList.copyOf(new ArrayList<>(ImmutableList.of(1)));
              }
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }
}
