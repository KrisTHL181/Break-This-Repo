package net.minecraft.world.item.slot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public record SlotSourceReference(ResourceKey<SlotSource> name) implements SlotSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SlotSourceReference> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ResourceKey.codec(Registries.SLOT_SOURCE).fieldOf("name").forGetter(SlotSourceReference::name)).apply(i, SlotSourceReference::new)
   );

   @Override
   public MapCodec<SlotSourceReference> codec() {
      return MAP_CODEC;
   }

   @Override
   public SlotCollection provide(final LootContext context) {
      SlotSource slotSource = context.getResolver().get(this.name).map(Holder::value).orElse(null);
      if (slotSource == null) {
         LOGGER.warn("Unknown slot source: {}", this.name.identifier());
         return SlotCollection.EMPTY;
      }

      LootContext.VisitedEntry<?> breadcrumb = LootContext.createVisitedEntry(slotSource);
      if (context.pushVisitedElement(breadcrumb)) {
         try {
            return slotSource.provide(context);
         } finally {
            context.popVisitedElement(breadcrumb);
         }
      } else {
         LOGGER.warn("Detected infinite loop in slot source");
         return SlotCollection.EMPTY;
      }
   }

   @Override
   public void validate(final ValidationContext context) {
      SlotSource.super.validate(context);
      Validatable.validateReference(context, this.name);
   }
}
