package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;

public record StorageNbtProvider(Identifier id) implements NbtProvider {
   public static final MapCodec<StorageNbtProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Identifier.CODEC.fieldOf("source").forGetter(StorageNbtProvider::id)).apply(i, StorageNbtProvider::new)
   );

   @Override
   public MapCodec<StorageNbtProvider> codec() {
      return MAP_CODEC;
   }

   @Override
   public Tag get(final LootContext context) {
      return context.getLevel().getServer().getCommandStorage().get(this.id);
   }
}
