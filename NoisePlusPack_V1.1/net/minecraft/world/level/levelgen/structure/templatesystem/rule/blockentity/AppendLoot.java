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
      p_391094_ -> p_391094_.group(LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(p_327505_ -> p_327505_.lootTable)).apply(p_391094_, AppendLoot::new)
   );
   private final ResourceKey<LootTable> lootTable;

   public AppendLoot(ResourceKey<LootTable> p_334648_) {
      this.lootTable = p_334648_;
   }

   @Override
   public CompoundTag apply(RandomSource p_277994_, @Nullable CompoundTag p_277854_) {
      CompoundTag compoundtag = p_277854_ == null ? new CompoundTag() : p_277854_.copy();
      compoundtag.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
      compoundtag.putLong("LootTableSeed", p_277994_.nextLong());
      return compoundtag;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_LOOT;
   }
}
