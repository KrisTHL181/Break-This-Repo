package net.minecraft.data.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.worldgen.BootstrapContext;

public abstract class AdvancementSubProvider {
   protected final BootstrapContext<Advancement> output;

   protected AdvancementSubProvider(final BootstrapContext<Advancement> output) {
      this.output = output;
   }

   public abstract void generate();

   @FunctionalInterface
   public interface Factory {
      AdvancementSubProvider create(BootstrapContext<Advancement> output);
   }
}
