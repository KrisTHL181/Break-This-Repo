package net.minecraft.data.advancements;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

public interface AdvancementSubProvider {
   void generate(HolderLookup.Provider var1, Consumer<AdvancementHolder> var2);

   static AdvancementHolder createPlaceholder(String p_312736_) {
      return Advancement.Builder.advancement().build(Identifier.parse(p_312736_));
   }
}
