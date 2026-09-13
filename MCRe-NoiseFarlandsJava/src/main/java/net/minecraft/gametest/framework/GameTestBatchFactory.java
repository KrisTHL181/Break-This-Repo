package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class GameTestBatchFactory {
    private static final int MAX_TESTS_PER_BATCH = 50;
    public static final GameTestBatchFactory.TestDecorator DIRECT = (test, level) -> Stream.of(
        new GameTestInfo(test, Rotation.NONE, level, RetryOptions.noRetries())
    );

    public static List<GameTestBatch> divideIntoBatches(
        final Collection<Holder.Reference<GameTestInstance>> allTests, final GameTestBatchFactory.TestDecorator decorator, final ServerLevel level
    ) {
        Map<Holder<TestEnvironmentDefinition<?>>, List<GameTestInfo>> testsPerBatch = allTests.stream()
            .flatMap(test -> decorator.decorate((Holder.Reference<GameTestInstance>)test, level))
            .collect(Collectors.groupingBy(info -> info.getTest().batch()));
        return testsPerBatch.entrySet().stream().flatMap(e -> {
            Holder<TestEnvironmentDefinition<?>> batchKey = e.getKey();
            List<GameTestInfo> testsInBatch = e.getValue();
            return Streams.mapWithIndex(Lists.partition(testsInBatch, 50).stream(), (tests, index) -> toGameTestBatch(tests, batchKey, (int)index));
        }).toList();
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
        return fromGameTestInfo(50);
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo(final int maxTestsPerBatch) {
        return gameTestInfos -> {
            Map<Holder<TestEnvironmentDefinition<?>>, List<GameTestInfo>> testFunctionsPerBatch = gameTestInfos.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(gameTestInfo -> gameTestInfo.getTest().batch()));
            return testFunctionsPerBatch.entrySet()
                .stream()
                .flatMap(
                    e -> {
                        Holder<TestEnvironmentDefinition<?>> batchKey = e.getKey();
                        List<GameTestInfo> testsInBatch = e.getValue();
                        return Streams.mapWithIndex(
                            Lists.partition(testsInBatch, maxTestsPerBatch).stream(),
                            (tests, index) -> toGameTestBatch(List.copyOf((Collection<? extends GameTestInfo>)tests), batchKey, (int)index)
                        );
                    }
                )
                .toList();
        };
    }

    public static GameTestBatch toGameTestBatch(final Collection<GameTestInfo> tests, final Holder<TestEnvironmentDefinition<?>> batch, final int counter) {
        return new GameTestBatch(counter, tests, batch);
    }

    @FunctionalInterface
    public interface TestDecorator {
        Stream<GameTestInfo> decorate(Holder.Reference<GameTestInstance> test, ServerLevel level);
    }
}