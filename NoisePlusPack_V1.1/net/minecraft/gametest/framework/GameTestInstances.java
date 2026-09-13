package net.minecraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface GameTestInstances {
   ResourceKey<GameTestInstance> ALWAYS_PASS = create("always_pass");

   static void bootstrap(BootstrapContext<GameTestInstance> p_393955_) {
      HolderGetter<Consumer<GameTestHelper>> holdergetter = p_393955_.lookup(Registries.TEST_FUNCTION);
      HolderGetter<TestEnvironmentDefinition> holdergetter1 = p_393955_.lookup(Registries.TEST_ENVIRONMENT);
      p_393955_.register(
         ALWAYS_PASS,
         new FunctionGameTestInstance(
            BuiltinTestFunctions.ALWAYS_PASS,
            new TestData<>(holdergetter1.getOrThrow(GameTestEnvironments.DEFAULT_KEY), Identifier.withDefaultNamespace("empty"), 1, 1, false)
         )
      );
   }

   private static ResourceKey<GameTestInstance> create(String p_395470_) {
      return ResourceKey.create(Registries.TEST_INSTANCE, Identifier.withDefaultNamespace(p_395470_));
   }
}
