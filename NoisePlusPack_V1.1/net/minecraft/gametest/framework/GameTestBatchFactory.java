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
   public static final GameTestBatchFactory.TestDecorator DIRECT = (p_389747_, p_389748_) -> Stream.of(
      new GameTestInfo(p_389747_, Rotation.NONE, p_389748_, RetryOptions.noRetries())
   );

   public static List<GameTestBatch> divideIntoBatches(
      Collection<Holder.Reference<GameTestInstance>> p_393682_, GameTestBatchFactory.TestDecorator p_393344_, ServerLevel p_397934_
   ) {
      Map<Holder<TestEnvironmentDefinition>, List<GameTestInfo>> map = p_393682_.stream()
         .flatMap(p_389738_ -> p_393344_.decorate((Holder.Reference<GameTestInstance>)p_389738_, p_397934_))
         .collect(Collectors.groupingBy(p_389739_ -> p_389739_.getTest().batch()));
      return map.entrySet().stream().flatMap(p_389740_ -> {
         Holder<TestEnvironmentDefinition> holder = p_389740_.getKey();
         List<GameTestInfo> list = p_389740_.getValue();
         return Streams.mapWithIndex(Lists.partition(list, 50).stream(), (p_389734_, p_389735_) -> toGameTestBatch(p_389734_, holder, (int)p_389735_));
      }).toList();
   }

   public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
      return fromGameTestInfo(50);
   }

   public static GameTestRunner.GameTestBatcher fromGameTestInfo(int p_344529_) {
      return p_341088_ -> {
         Map<Holder<TestEnvironmentDefinition>, List<GameTestInfo>> map = p_341088_.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(p_389744_ -> p_389744_.getTest().batch()));
         return map.entrySet()
            .stream()
            .flatMap(
               p_389746_ -> {
                  Holder<TestEnvironmentDefinition> holder = p_389746_.getKey();
                  List<GameTestInfo> list = p_389746_.getValue();
                  return Streams.mapWithIndex(
                     Lists.partition(list, p_344529_).stream(), (p_389742_, p_389743_) -> toGameTestBatch(List.copyOf(p_389742_), holder, (int)p_389743_)
                  );
               }
            )
            .toList();
      };
   }

   public static GameTestBatch toGameTestBatch(Collection<GameTestInfo> p_342407_, Holder<TestEnvironmentDefinition> p_397533_, int p_396485_) {
      return new GameTestBatch(p_396485_, p_342407_, p_397533_);
   }

   @FunctionalInterface
   public interface TestDecorator {
      Stream<GameTestInfo> decorate(Holder.Reference<GameTestInstance> var1, ServerLevel var2);
   }
}
