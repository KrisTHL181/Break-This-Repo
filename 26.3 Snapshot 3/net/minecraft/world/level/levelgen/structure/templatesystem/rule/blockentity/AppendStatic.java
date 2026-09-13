package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class AppendStatic implements RuleBlockEntityModifier {
   public static final MapCodec<AppendStatic> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(CompoundTag.CODEC.fieldOf("data").forGetter(r -> r.tag)).apply(i, AppendStatic::new)
   );
   private final CompoundTag tag;

   public AppendStatic(final CompoundTag tag) {
      this.tag = tag;
   }

   @Override
   public CompoundTag apply(final RandomSource random, final @Nullable CompoundTag existingTag) {
      return existingTag == null ? this.tag.copy() : existingTag.merge(this.tag);
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_STATIC;
   }
}
