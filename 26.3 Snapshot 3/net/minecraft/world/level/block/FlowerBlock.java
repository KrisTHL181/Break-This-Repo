package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FlowerBlock extends VegetationBlock implements SuspiciousEffectHolder {
   private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 10.0);
   private final SuspiciousStewEffects suspiciousStewEffects;

   public FlowerBlock(final Holder<MobEffect> suspiciousStewEffect, final float effectSeconds, final BlockBehaviour.Properties properties) {
      this(makeEffectList(suspiciousStewEffect, effectSeconds), properties);
   }

   public FlowerBlock(final SuspiciousStewEffects suspiciousStewEffects, final BlockBehaviour.Properties properties) {
      super(properties);
      this.suspiciousStewEffects = suspiciousStewEffects;
   }

   protected static SuspiciousStewEffects makeEffectList(final Holder<MobEffect> suspiciousStewEffect, final float effectSeconds) {
      return new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(suspiciousStewEffect, Mth.floor(effectSeconds * 20.0F))));
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE.move(state.getOffset(pos));
   }

   @Override
   public SuspiciousStewEffects getSuspiciousEffects() {
      return this.suspiciousStewEffects;
   }

   public @Nullable MobEffectInstance getBeeInteractionEffect() {
      return null;
   }
}
