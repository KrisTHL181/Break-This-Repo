package net.minecraft.gametest.framework;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface GameTestEnvironments {
   String DEFAULT = "default";
   ResourceKey<TestEnvironmentDefinition> DEFAULT_KEY = create("default");

   private static ResourceKey<TestEnvironmentDefinition> create(String p_397510_) {
      return ResourceKey.create(Registries.TEST_ENVIRONMENT, Identifier.withDefaultNamespace(p_397510_));
   }

   static void bootstrap(BootstrapContext<TestEnvironmentDefinition> p_396849_) {
      p_396849_.register(DEFAULT_KEY, new TestEnvironmentDefinition.AllOf(List.of()));
   }
}
