package com.ivi.juc.code.async;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author lancer
 * @Date 2023/3/11 22:58
 * @Description
 */
public class FutureUtils {

    public static <T> CompletableFuture<T> failed(Throwable ex) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        cf.completeExceptionally(ex);
        return cf;
    }

    public static <T> CompletableFuture<T> success(T result) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        cf.complete(result);
        return cf;
    }

    /**
     * List<CompletableFuture<T>> -> CompletableFuture<List<T>>
     */
    public static <T> CompletableFuture<List<T>> sequence(Collection<CompletableFuture<T>> cfs) {
        return sequence(cfs, Objects::nonNull);
    }

    public static <T> CompletableFuture<List<T>> sequence(
            Collection<CompletableFuture<T>> cfs,
            Predicate<? super T> filterFunction) {
        return CompletableFuture
                .allOf(cfs.toArray(new CompletableFuture<?>[0]))
                .thenApply(
                        v -> cfs.stream()
                                .map(CompletableFuture::join)
                                .filter(filterFunction)
                                .collect(Collectors.toList()));
    }

    /**
     * List<CompletableFuture<List<T>>> -> CompletableFuture<List<T>>
     * 多用于分页查询的场景
     */
    public static <T> CompletableFuture<List<T>> sequenceList(Collection<CompletableFuture<List<T>>> cfs) {
        return sequenceList(cfs, Objects::nonNull);
    }

    public static <T> CompletableFuture<List<T>> sequenceList(
            Collection<CompletableFuture<List<T>>> cfs,
            Predicate<? super T> filterFunction) {
        return CompletableFuture
                .allOf(cfs.toArray(new CompletableFuture<?>[0]))
                .thenApply(
                        v -> cfs.stream()
                                .flatMap(
                                        listCompletableFuture ->
                                                listCompletableFuture.join().stream()
                                                        .filter(filterFunction))
                                .collect(Collectors.toList()));
    }

    /**
     * List<CompletableFuture<Map<K, V>>> -> CompletableFuture<Map<K, V>>
     */
    public static <K, V> CompletableFuture<Map<K, V>> sequenceMap(Collection<CompletableFuture<Map<K, V>>> cfs) {
        return sequenceMap(cfs, (a, b) -> b);
    }

    public static <K, V> CompletableFuture<Map<K, V>> sequenceMap(
            Collection<CompletableFuture<Map<K, V>>> cfs,
            // 自定义key冲突时的merge策略
            BinaryOperator<V> mergeFunction) {
        return CompletableFuture
                .allOf(cfs.toArray(new CompletableFuture<?>[0]))
                .thenApply(
                        v -> cfs.stream()
                                .map(CompletableFuture::join)
                                .flatMap(map -> map.entrySet().stream())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction)));
    }
}
