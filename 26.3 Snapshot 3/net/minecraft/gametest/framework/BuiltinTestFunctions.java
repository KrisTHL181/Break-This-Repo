package net.minecraft.gametest.framework;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class BuiltinTestFunctions extends TestFunctionLoader {
   public static final ResourceKey<Consumer<GameTestHelper>> ALWAYS_PASS = create("always_pass");
   public static final Consumer<GameTestHelper> ALWAYS_PASS_INSTANCE = GameTestHelper::succeed;

   private static ResourceKey<Consumer<GameTestHelper>> create(final String name) {
      return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.withDefaultNamespace(name));
   }

   public static Consumer<GameTestHelper> bootstrap(final Registry<Consumer<GameTestHelper>> registry) {
      registerLoader(new BuiltinTestFunctions());
      runLoaders(registry);
      return ALWAYS_PASS_INSTANCE;
   }

   @Override
   public void load(final BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> register) {
      register.accept(ALWAYS_PASS, ALWAYS_PASS_INSTANCE);
   }
}
