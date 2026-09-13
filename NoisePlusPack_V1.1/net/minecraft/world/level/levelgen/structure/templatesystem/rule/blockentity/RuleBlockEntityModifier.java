package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public interface RuleBlockEntityModifier {
   Codec<RuleBlockEntityModifier> CODEC = BuiltInRegistries.RULE_BLOCK_ENTITY_MODIFIER
      .byNameCodec()
      .dispatch(RuleBlockEntityModifier::getType, RuleBlockEntityModifierType::codec);

   @Nullable CompoundTag apply(RandomSource var1, @Nullable CompoundTag var2);

   RuleBlockEntityModifierType<?> getType();
}
