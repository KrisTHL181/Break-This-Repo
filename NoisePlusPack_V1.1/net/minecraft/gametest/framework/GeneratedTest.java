package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record GeneratedTest(
   Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition>>> tests,
   ResourceKey<Consumer<GameTestHelper>> functionKey,
   Consumer<GameTestHelper> function
) {
   public GeneratedTest(Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition>>> p_453374_, Identifier p_453964_, Consumer<GameTestHelper> p_391901_) {
      this(p_453374_, ResourceKey.create(Registries.TEST_FUNCTION, p_453964_), p_391901_);
   }

   public GeneratedTest(Identifier p_451283_, TestData<ResourceKey<TestEnvironmentDefinition>> p_460286_, Consumer<GameTestHelper> p_394808_) {
      this(Map.of(p_451283_, p_460286_), p_451283_, p_394808_);
   }
}
