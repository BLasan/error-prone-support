package tech.picnic.errorprone.refastertemplates;

import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.MayOptionallyUse;
import com.google.errorprone.refaster.annotation.Placeholder;
import com.google.errorprone.refaster.annotation.Repeated;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** The Refaster templates for the migration of RxJava to Reactor */
public final class RxJavaToReactorTemplates {
  private RxJavaToReactorTemplates() {}

  static final class FluxToFlowableToFlux<T> {
    @BeforeTemplate
    Flux<T> before(Flux<T> flux, BackpressureStrategy strategy) {
      return Refaster.anyOf(
          flux.as(RxJava2Adapter::fluxToObservable)
              .toFlowable(strategy)
              .as(RxJava2Adapter::flowableToFlux),
          flux.as(RxJava2Adapter::fluxToFlowable).as(RxJava2Adapter::flowableToFlux));
    }

    @AfterTemplate
    Flux<T> after(Flux<T> flux) {
      return flux;
    }
  }

  // XXX: This wouldn't work for this case right?
  //     return Flowable.combineLatest(
  //  getEnabledConsentRequests(requiredConsentTopics, locale), // returns Flowable
  //            Flowable.fromIterable(requiredConsentTopics), // returns Flowable
  //          this::filterByTopic)
  static final class FlowableCombineLatest<T1, T2, R> {
    @BeforeTemplate
    Flowable<R> before(
        Publisher<? extends T1> p1,
        Publisher<? extends T2> p2,
        BiFunction<? super T1, ? super T2, ? extends R> biFunction) {
      return Flowable.combineLatest(p1, p2, biFunction);
    }

    @AfterTemplate
    Flowable<R> after(
        Publisher<? extends T1> p1,
        Publisher<? extends T2> p2,
        java.util.function.BiFunction<? super T1, ? super T2, ? extends R> biFunction) {
      return RxJava2Adapter.fluxToFlowable(Flux.combineLatest(p1, p2, biFunction));
    }
  }

  static final class FlowableConcatWithPublisher<T> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Publisher<T> source) {
      return flowable.concatWith(source);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, Publisher<T> source) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .concatWith(source)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: Flowable.concatWith. -> CompletableSource
  // XXX: Flowable.concatWith. -> SingleSource
  // XXX: Flowable.concatWith. -> MaybeSource

  abstract static class FlowableDefer<T> {
    @Placeholder
    abstract Flowable<T> flowableCallable();

    @BeforeTemplate
    Flowable<T> before() {
      return Flowable.defer(() -> flowableCallable());
    }

    @AfterTemplate
    Flowable<T> after() {
      return RxJava2Adapter.fluxToFlowable(
          Flux.defer(() -> flowableCallable().as(RxJava2Adapter::flowableToFlux)));
    }
  }

  static class FlowableEmpty<T> {
    @BeforeTemplate
    Flowable<T> before() {
      return Flowable.empty();
    }

    @AfterTemplate
    Flowable<T> after() {
      return RxJava2Adapter.fluxToFlowable(Flux.empty());
    }
  }

  static class FlowableErrorThrowable<T> {
    @BeforeTemplate
    Flowable<T> before(Throwable throwable) {
      return Flowable.error(throwable);
    }

    @AfterTemplate
    Flowable<T> after(Throwable throwable) {
      return RxJava2Adapter.fluxToFlowable(Flux.error(throwable));
    }
  }

  // XXX: Use `CanBeCoercedTo`.
  static class FlowableErrorCallable<T> {
    @BeforeTemplate
    Flowable<T> before(Callable<? extends Throwable> throwable) {
      return Flowable.error(throwable);
    }

    @AfterTemplate
    Flowable<T> after(Supplier<? extends Throwable> throwable) {
      return RxJava2Adapter.fluxToFlowable(Flux.error(throwable));
    }
  }

  static final class FlowableJust<T> {
    @BeforeTemplate
    Flowable<T> before(T t, @Repeated T arguments) {
      return Flowable.just(t, arguments);
    }

    @AfterTemplate
    Flowable<T> after(T t, @Repeated T arguments) {
      return RxJava2Adapter.fluxToFlowable(Flux.just(t, arguments));
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableFilter<S, T extends S> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Predicate<S> predicate) {
      return flowable.filter(predicate);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, java.util.function.Predicate<S> predicate) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .filter(predicate)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  static final class FlowableFirstElement<T> {
    @BeforeTemplate
    Maybe<T> before(Flowable<T> flowable) {
      return flowable.firstElement();
    }

    @AfterTemplate
    Maybe<T> after(Flowable<T> flowable) {
      return flowable.as(RxJava2Adapter::flowableToFlux).next().as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableFlatMap<I, T extends I, O, P extends Publisher<? extends O>> {
    @BeforeTemplate
    Flowable<O> before(Flowable<T> flowable, Function<I, P> function) {
      return flowable.flatMap(function);
    }

    @AfterTemplate
    Flowable<O> after(Flowable<I> flowable, java.util.function.Function<I, P> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .flatMap(function)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

//  static final class FlowableFromArray<T> {
//    @BeforeTemplate
//
//  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class FlowableMap<I, T extends I, O> {
    @BeforeTemplate
    Flowable<O> before(Flowable<T> flowable, Function<I, O> function) {
      return flowable.map(function);
    }

    @AfterTemplate
    Flowable<O> after(Flowable<T> flowable, java.util.function.Function<I, O> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .map(function)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  static final class FlowableToMap<I, T extends I, O> {
    @BeforeTemplate
    Single<Map<O, T>> before(Flowable<T> flowable, Function<I, O> function) {
      return flowable.toMap(function);
    }

    @AfterTemplate
    Single<Map<O, T>> after(Flowable<T> flowable, java.util.function.Function<I, O> function) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .collectMap(function)
          .as(RxJava2Adapter::monoToSingle);
    }
  }

  static final class FlowableSwitchIfEmptyPublisher<T> {
    @BeforeTemplate
    Flowable<T> before(Flowable<T> flowable, Publisher<T> publisher) {
      return flowable.switchIfEmpty(publisher);
    }

    @AfterTemplate
    Flowable<T> after(Flowable<T> flowable, Publisher<T> publisher) {
      return flowable
          .as(RxJava2Adapter::flowableToFlux)
          .switchIfEmpty(publisher)
          .as(RxJava2Adapter::fluxToFlowable);
    }
  }

  // XXX: Is this conversion correct?
  static class MaybeAmb<T> {
    @BeforeTemplate
    Maybe<T> before(Iterable<? extends MaybeSource<? extends T>> iterable) {
      return Maybe.amb(iterable);
    }

    @AfterTemplate
    Maybe<T> after(Iterable<? extends Mono<? extends T>> iterable) {
      return Mono.firstWithSignal(iterable).as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: Validate.... This is not correct yet. See `maybeSource` in aftertemplate.
  static class MaybeAmbWith<T> {
    @BeforeTemplate
    Maybe<T> before(Maybe<T> maybe, MaybeSource<? extends T> maybeSource) {
      return maybe.ambWith(maybeSource);
    }

    @AfterTemplate
    Maybe<T> after(Maybe<T> maybe, Mono<T> maybeSource) {
      return maybe.as(RxJava2Adapter::maybeToMono).or(maybeSource).as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: Perhaps omit this one.
  abstract static class MaybeDeferToMono<T> {
    @Placeholder
    abstract Maybe<T> maybeProducer();

    @BeforeTemplate
    Mono<T> before() {
      return Maybe.defer(() -> maybeProducer()).as(RxJava2Adapter::maybeToMono);
    }

    @AfterTemplate
    Mono<T> after() {
      return Mono.defer(() -> maybeProducer().as(RxJava2Adapter::maybeToMono));
    }
  }

  static final class MaybeCast<T> {
    @BeforeTemplate
    Maybe<T> before(Maybe<T> maybe) {
      return maybe.cast(Refaster.<T>clazz());
    }

    @AfterTemplate
    Maybe<T> after(Maybe<T> maybe) {
      return maybe;
    }
  }

  static final class MaybeWrap<T> {
    @BeforeTemplate
    Maybe<T> before(Maybe<T> maybe) {
      return Maybe.wrap(maybe);
    }

    @AfterTemplate
    Maybe<T> after(Maybe<T> maybe) {
      return maybe;
    }
  }

  // See the MyUtil for additional explanation.
  static final class MaybeFlatMapFunction<I, T extends I, O, M extends MaybeSource<? extends O>> {
    @BeforeTemplate
    Maybe<O> before(Maybe<T> maybe, Function<I, M> function) {
      return maybe.flatMap(function);
    }

    @AfterTemplate
    @SuppressWarnings("unchecked")
    Maybe<O> after(Maybe<T> maybe, Function<I, M> function) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .flatMap(
              v ->
                  RxJava2Adapter.maybeToMono(
                      Maybe.wrap((Maybe<O>) MyUtil.convert(function).apply(v))))
          .as(RxJava2Adapter::monoToMaybe);
    }
  }

  abstract static class MaybeFlatMapLambda<S, T> {
    @Placeholder
    abstract Maybe<T> toMaybeFunction(@MayOptionallyUse S element);

    @BeforeTemplate
    Maybe<T> before(Maybe<S> maybe) {
      return maybe.flatMap(v -> toMaybeFunction(v));
    }

    @AfterTemplate
    Maybe<T> after(Maybe<S> maybe) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .flatMap(v -> toMaybeFunction(v).as(RxJava2Adapter::maybeToMono))
          .as(RxJava2Adapter::monoToMaybe);
    }
  }

  //  The following template is required to rewrite this code from platform:
  //    private Completable verifyTagExists(Optional<String> tagId) {
  //    return Maybe.defer(() -> tagId.map(Maybe::just).orElseGet(Maybe::empty))
  //        .flatMapSingleElement(this::getTagById)
  //        .ignoreElement();
  //    }
  //    static final class MaybeFlatMapSingleElement<
  //        I, T extends I, O, P extends SingleSource<? extends O>> { // <S, T extends S, O> {
  //      @BeforeTemplate
  //      Maybe<O> before(Maybe<T> maybe, Function<I, P> function) {
  //        return maybe.flatMapSingleElement(function);
  //      }
  //
  //      @AfterTemplate
  //      Maybe<O> after(Maybe<T> maybe, java.util.function.Function<I, P> function) {
  //        return maybe
  //            .as(RxJava2Adapter::maybeToMono)
  //            .flatMap(function)
  //            .as(RxJava2Adapter::monoToMaybe);
  //      }
  //    }

  static final class MaybeIgnoreElement<T> {
    @BeforeTemplate
    Completable before(Maybe<T> maybe) {
      return maybe.ignoreElement();
    }

    @AfterTemplate
    Completable after(Maybe<T> maybe) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .ignoreElement()
          .as(RxJava2Adapter::monoToCompletable);
    }
  }

  static final class MaybeSwitchIfEmpty<S, T extends S> {
    @BeforeTemplate
    Single<S> before(Maybe<S> maybe, Single<T> single) {
      return maybe.switchIfEmpty(single);
    }

    @AfterTemplate
    Single<S> after(Maybe<S> maybe, Single<T> single) {
      return maybe
          .as(RxJava2Adapter::maybeToMono)
          .switchIfEmpty(single.as(RxJava2Adapter::singleToMono))
          .as(RxJava2Adapter::monoToSingle);
    }
  }

  static final class RemoveRedundantCast<T> {
    @BeforeTemplate
    T before(T object) {
      return (T) object;
    }

    @AfterTemplate
    T after(T object) {
      return object;
    }
  }

  // XXX: What should we do with the naming here? Since it is not entirely correct now.
  static final class MonoToFlowableToMono<T> {
    @BeforeTemplate
    Mono<Void> before(Mono<Void> mono) {
      return mono.as(RxJava2Adapter::monoToCompletable).as(RxJava2Adapter::completableToMono);
    }

    @BeforeTemplate
    Mono<T> before2(Mono<T> mono) {
      return Refaster.anyOf(
          mono.as(RxJava2Adapter::monoToMaybe).as(RxJava2Adapter::maybeToMono),
          mono.as(RxJava2Adapter::monoToSingle).as(RxJava2Adapter::singleToMono));
    }

    @AfterTemplate
    Mono<T> after(Mono<T> mono) {
      return mono;
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class SingleFilter<S, T extends S> {
    @BeforeTemplate
    Maybe<T> before(Single<T> single, Predicate<S> predicate) {
      return single.filter(predicate);
    }

    @AfterTemplate
    Maybe<T> after(Single<T> single, java.util.function.Predicate<S> predicate) {
      return single
          .as(RxJava2Adapter::singleToMono)
          .filter(predicate)
          .as(RxJava2Adapter::monoToMaybe);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  abstract static class SingleFlatMapLambda<S, T> {
    @Placeholder
    abstract Single<T> toSingleFunction(@MayOptionallyUse S element);

    @BeforeTemplate
    Single<T> before(Single<S> single) {
      return single.flatMap(v -> toSingleFunction(v));
    }

    @AfterTemplate
    Single<T> after(Single<S> single) {
      return single
          .as(RxJava2Adapter::singleToMono)
          .flatMap(v -> toSingleFunction(v).as(RxJava2Adapter::singleToMono))
          .as(RxJava2Adapter::monoToSingle);
    }
  }

  // XXX: `function` type change; look into `Refaster.canBeCoercedTo(...)`.
  static final class SingleMap<I, T extends I, O> {
    @BeforeTemplate
    Single<O> before(Single<T> single, Function<I, O> function) {
      return single.map(function);
    }

    @AfterTemplate
    Single<O> after(Single<T> single, java.util.function.Function<I, O> function) {
      return single.as(RxJava2Adapter::singleToMono).map(function).as(RxJava2Adapter::monoToSingle);
    }
  }

  //  @Matches(value = DoesNotThrowException.class, arguments = "java.lang.Exception")
  //  @interface DoesNotThrowCheckedException {}
  //
  //  abstract static class Test {
  //    @Placeholder
  //    @DoesNotThrowCheckedException
  //    abstract void operation();
  //
  //    @BeforeTemplate
  //    void before(ExecutorService es) {
  //      es.submit(
  //          () -> {
  //            operation();
  //            return null;
  //          });
  //    }
  //
  //    @AfterTemplate
  //    void after(ExecutorService es) {
  //      es.submit(() -> operation());
  //    }
  //  }
  //
  //  public class MethodThrowsExceptionTemplate<T> {
  //    @BeforeTemplate
  //    Mono<T> before(@DoesNotThrowCheckedException T obj) {
  //      return Mono.fromCallable(() -> obj);
  //    }
  //
  //    @AfterTemplate
  //    Mono<T> after(T obj) {
  //      return Mono.fromSupplier(() -> obj);
  //    }
  //  }
  //
  //  public class MethodThrowsExceptionTemplate<T> {
  //    @BeforeTemplate
  //    Mono<T> before(@Matches(DefersToExpressionWhichDoesNotThrow.class) Callable<T> callable) {
  //      return Mono.fromCallable(callable);
  //    }
  //
  //    @AfterTemplate
  //    Mono<T> after(Supplier<T> callable) {
  //      return Mono.fromSupplier(callable);
  //    }
  //  }

  /**
   * XXX: Temporary solution, this could be fixed when we know whether the function throws an
   * Exception.
   */
  public static final class MyUtil {

    private MyUtil() {}

    /**
     * Temporary construct to convert functions that do not throw an exception
     *
     * <p>The idea is to convert a io.reactivex.functions.Function to a java.util.function.Function.
     *
     * @param function The function to convert
     * @param <I> The input type
     * @param <O> The output type
     * @return the java.util.function.Function
     */
    @SuppressWarnings({"IllegalCatch", "NoFunctionalReturnType"})
    public static <I, O> java.util.function.Function<I, O> convert(
        Function<? super I, ? extends O> function) {
      return input -> {
        try {
          return function.apply(input);
        } catch (Exception e) {
          throw Exceptions.propagate(e);
        }
      };
    }
  }

  // "Coersion" (find better name):
  // instanceof (support this?)
  // two functional interfaces with:
  // B.return type extends A.return type
  // A.param 1 type extends B.param 1 type
  // ....
  // B throws a subset of the exceptions thrown by A

  //  @CheckParameterCoersion
  @SuppressWarnings("NoFunctionalReturnType")
  static final class UnnecessaryConversion<I, O> {
    @BeforeTemplate
    java.util.function.Function<I, O> before(Function<I, O> function) {
      return MyUtil.convert(function);
    }

    @AfterTemplate
    java.util.function.Function<I, O> after(java.util.function.Function<I, O> function) {
      return function;
    }
  }
}
