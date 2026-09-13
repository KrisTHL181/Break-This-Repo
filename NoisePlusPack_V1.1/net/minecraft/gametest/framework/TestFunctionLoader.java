package net.minecraft.gametest.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public abstract class TestFunctionLoader {
   private static final List<TestFunctionLoader> loaders = new ArrayList<>();

   public static void registerLoader(TestFunctionLoader p_397469_) {
      loaders.add(p_397469_);
   }

   public static void runLoaders(Registry<Consumer<GameTestHelper>> p_395511_) {
      for (TestFunctionLoader testfunctionloader : loaders) {
         testfunctionloader.load((p_396342_, p_395649_) -> Registry.register(p_395511_, p_396342_, p_395649_));
      }
   }

   public abstract void load(BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> var1);
}
