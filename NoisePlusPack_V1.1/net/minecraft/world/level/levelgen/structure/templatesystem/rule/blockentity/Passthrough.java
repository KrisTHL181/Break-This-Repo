package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class Passthrough implements RuleBlockEntityModifier {
   public static final Passthrough INSTANCE = new Passthrough();
   public static final MapCodec<Passthrough> CODEC = MapCodec.unit(INSTANCE);

   @Override
   public @Nullable CompoundTag apply(RandomSource p_277737_, @Nullable CompoundTag p_277665_) {
      return p_277665_;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.PASSTHROUGH;
   }
}
