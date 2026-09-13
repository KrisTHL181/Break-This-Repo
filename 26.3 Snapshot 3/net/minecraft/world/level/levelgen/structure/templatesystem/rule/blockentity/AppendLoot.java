package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class AppendLoot implements RuleBlockEntityModifier {
   public static final MapCodec<AppendLoot> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(c -> c.lootTable)).apply(i, AppendLoot::new)
   );
   private final ResourceKey<LootTable> lootTable;

   public AppendLoot(final ResourceKey<LootTable> lootTable) {
      this.lootTable = lootTable;
   }

   @Override
   public CompoundTag apply(final RandomSource random, final @Nullable CompoundTag existingTag) {
      CompoundTag result = existingTag == null ? new CompoundTag() : existingTag.copy();
      result.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
      result.putLong("LootTableSeed", random.nextLong());
      return result;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_LOOT;
   }
}
