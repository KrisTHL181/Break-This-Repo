package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record GeneratedTest(
   Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition<?>>>> tests,
   ResourceKey<Consumer<GameTestHelper>> functionKey,
   Consumer<GameTestHelper> function
) {
   public GeneratedTest(
      final Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition<?>>>> tests, final Identifier functionId, final Consumer<GameTestHelper> function
   ) {
      this(tests, ResourceKey.create(Registries.TEST_FUNCTION, functionId), function);
   }

   public GeneratedTest(final Identifier id, final TestData<ResourceKey<TestEnvironmentDefinition<?>>> testData, final Consumer<GameTestHelper> function) {
      this(Map.of(id, testData), id, function);
   }
}
