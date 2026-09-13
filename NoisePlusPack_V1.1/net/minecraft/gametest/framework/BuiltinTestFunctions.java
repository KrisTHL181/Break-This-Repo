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

   private static ResourceKey<Consumer<GameTestHelper>> create(String p_397663_) {
      return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.withDefaultNamespace(p_397663_));
   }

   public static Consumer<GameTestHelper> bootstrap(Registry<Consumer<GameTestHelper>> p_393257_) {
      registerLoader(new BuiltinTestFunctions());
      runLoaders(p_393257_);
      return ALWAYS_PASS_INSTANCE;
   }

   @Override
   public void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> p_394058_) {
      p_394058_.accept(ALWAYS_PASS, ALWAYS_PASS_INSTANCE);
   }
}
