package net.minecraft.world.level.block;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public interface SuspiciousEffectHolder {
   SuspiciousStewEffects getSuspiciousEffects();

   static List<SuspiciousEffectHolder> getAllEffectHolders() {
      return BuiltInRegistries.ITEM.stream().map(SuspiciousEffectHolder::tryGet).filter(Objects::nonNull).collect(Collectors.toList());
   }

   static @Nullable SuspiciousEffectHolder tryGet(final ItemLike item) {
      if (item.asItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SuspiciousEffectHolder effectHolder) {
         return effectHolder;
      } else {
         return item.asItem() instanceof SuspiciousEffectHolder effectHolder ? effectHolder : null;
      }
   }
}
