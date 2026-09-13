package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WitherRoseBlock extends FlowerBlock {
   public static final MapCodec<WitherRoseBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422148_ -> p_422148_.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(p_422148_, WitherRoseBlock::new)
   );

   @Override
   public MapCodec<WitherRoseBlock> codec() {
      return CODEC;
   }

   public WitherRoseBlock(Holder<MobEffect> p_330275_, float p_332609_, BlockBehaviour.Properties p_58236_) {
      this(makeEffectList(p_330275_, p_332609_), p_58236_);
   }

   public WitherRoseBlock(SuspiciousStewEffects p_333459_, BlockBehaviour.Properties p_310026_) {
      super(p_333459_, p_310026_);
   }

   @Override
   protected boolean mayPlaceOn(BlockState p_58248_, BlockGetter p_58249_, BlockPos p_58250_) {
      return super.mayPlaceOn(p_58248_, p_58249_, p_58250_) || p_58248_.is(Blocks.NETHERRACK) || p_58248_.is(Blocks.SOUL_SAND) || p_58248_.is(Blocks.SOUL_SOIL);
   }

   @Override
   public void animateTick(BlockState p_222687_, Level p_222688_, BlockPos p_222689_, RandomSource p_222690_) {
      VoxelShape voxelshape = this.getShape(p_222687_, p_222688_, p_222689_, CollisionContext.empty());
      Vec3 vec3 = voxelshape.bounds().getCenter();
      double d0 = p_222689_.getX() + vec3.x;
      double d1 = p_222689_.getZ() + vec3.z;

      for (int i = 0; i < 3; i++) {
         if (p_222690_.nextBoolean()) {
            p_222688_.addParticle(
               ParticleTypes.SMOKE,
               d0 + p_222690_.nextDouble() / 5.0,
               p_222689_.getY() + (0.5 - p_222690_.nextDouble()),
               d1 + p_222690_.nextDouble() / 5.0,
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   @Override
   protected void entityInside(BlockState p_58238_, Level p_58239_, BlockPos p_58240_, Entity p_58241_, InsideBlockEffectApplier p_393607_, boolean p_432052_) {
      if (p_58239_ instanceof ServerLevel serverlevel
         && p_58239_.getDifficulty() != Difficulty.PEACEFUL
         && p_58241_ instanceof LivingEntity livingentity
         && !livingentity.isInvulnerableTo(serverlevel, p_58239_.damageSources().wither())) {
         livingentity.addEffect(this.getBeeInteractionEffect());
      }
   }

   @Override
   public MobEffectInstance getBeeInteractionEffect() {
      return new MobEffectInstance(MobEffects.WITHER, 40);
   }
}
