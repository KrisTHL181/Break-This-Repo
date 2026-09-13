package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class EyeblossomBlock extends FlowerBlock {
   public static final MapCodec<EyeblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422108_ -> p_422108_.group(Codec.BOOL.fieldOf("open").forGetter(p_378496_ -> p_378496_.type.open), propertiesCodec())
         .apply(p_422108_, EyeblossomBlock::new)
   );
   private static final int EYEBLOSSOM_XZ_RANGE = 3;
   private static final int EYEBLOSSOM_Y_RANGE = 2;
   private final EyeblossomBlock.Type type;

   @Override
   public MapCodec<? extends EyeblossomBlock> codec() {
      return CODEC;
   }

   public EyeblossomBlock(EyeblossomBlock.Type p_377758_, BlockBehaviour.Properties p_377813_) {
      super(p_377758_.effect, p_377758_.effectDuration, p_377813_);
      this.type = p_377758_;
   }

   public EyeblossomBlock(boolean p_377179_, BlockBehaviour.Properties p_375538_) {
      super(EyeblossomBlock.Type.fromBoolean(p_377179_).effect, EyeblossomBlock.Type.fromBoolean(p_377179_).effectDuration, p_375538_);
      this.type = EyeblossomBlock.Type.fromBoolean(p_377179_);
   }

   @Override
   public void animateTick(BlockState p_378124_, Level p_378091_, BlockPos p_377687_, RandomSource p_377934_) {
      if (this.type.emitSounds() && p_377934_.nextInt(700) == 0) {
         BlockState blockstate = p_378091_.getBlockState(p_377687_.below());
         if (blockstate.is(Blocks.PALE_MOSS_BLOCK)) {
            p_378091_.playLocalSound(p_377687_.getX(), p_377687_.getY(), p_377687_.getZ(), SoundEvents.EYEBLOSSOM_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }
      }
   }

   @Override
   protected void randomTick(BlockState p_377061_, ServerLevel p_376852_, BlockPos p_376526_, RandomSource p_377682_) {
      if (this.tryChangingState(p_377061_, p_376852_, p_376526_, p_377682_)) {
         p_376852_.playSound(null, p_376526_, this.type.transform().longSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      super.randomTick(p_377061_, p_376852_, p_376526_, p_377682_);
   }

   @Override
   protected void tick(BlockState p_378472_, ServerLevel p_377898_, BlockPos p_376262_, RandomSource p_378553_) {
      if (this.tryChangingState(p_378472_, p_377898_, p_376262_, p_378553_)) {
         p_377898_.playSound(null, p_376262_, this.type.transform().shortSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      super.tick(p_378472_, p_377898_, p_376262_, p_378553_);
   }

   private boolean tryChangingState(BlockState p_378680_, ServerLevel p_377734_, BlockPos p_375393_, RandomSource p_375792_) {
      boolean flag = p_377734_.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, p_375393_).toBoolean(this.type.open);
      if (flag == this.type.open) {
         return false;
      }

      EyeblossomBlock.Type eyeblossomblock$type = this.type.transform();
      p_377734_.setBlock(p_375393_, eyeblossomblock$type.state(), 3);
      p_377734_.gameEvent(GameEvent.BLOCK_CHANGE, p_375393_, GameEvent.Context.of(p_378680_));
      eyeblossomblock$type.spawnTransformParticle(p_377734_, p_375393_, p_375792_);
      BlockPos.betweenClosed(p_375393_.offset(-3, -2, -3), p_375393_.offset(3, 2, 3)).forEach(p_377124_ -> {
         BlockState blockstate = p_377734_.getBlockState(p_377124_);
         if (blockstate == p_378680_) {
            double d0 = Math.sqrt(p_375393_.distSqr(p_377124_));
            int i = p_375792_.nextIntBetweenInclusive((int)(d0 * 5.0), (int)(d0 * 10.0));
            p_377734_.scheduleTick(p_377124_, p_378680_.getBlock(), i);
         }
      });
      return true;
   }

   @Override
   protected void entityInside(
      BlockState p_375775_, Level p_376791_, BlockPos p_376904_, Entity p_376719_, InsideBlockEffectApplier p_391726_, boolean p_432037_
   ) {
      if (!p_376791_.isClientSide()
         && p_376791_.getDifficulty() != Difficulty.PEACEFUL
         && p_376719_ instanceof Bee bee
         && Bee.attractsBees(p_375775_)
         && !bee.hasEffect(MobEffects.POISON)) {
         bee.addEffect(this.getBeeInteractionEffect());
      }
   }

   @Override
   public MobEffectInstance getBeeInteractionEffect() {
      return new MobEffectInstance(MobEffects.POISON, 25);
   }

   public enum Type {
      OPEN(true, MobEffects.BLINDNESS, 11.0F, SoundEvents.EYEBLOSSOM_OPEN_LONG, SoundEvents.EYEBLOSSOM_OPEN, 16545810),
      CLOSED(false, MobEffects.NAUSEA, 7.0F, SoundEvents.EYEBLOSSOM_CLOSE_LONG, SoundEvents.EYEBLOSSOM_CLOSE, 6250335);

      final boolean open;
      final Holder<MobEffect> effect;
      final float effectDuration;
      final SoundEvent longSwitchSound;
      final SoundEvent shortSwitchSound;
      private final int particleColor;

      Type(
         final boolean p_378579_,
         final Holder<MobEffect> p_376708_,
         final float p_377657_,
         final SoundEvent p_376837_,
         final SoundEvent p_378282_,
         final int p_375732_
      ) {
         this.open = p_378579_;
         this.effect = p_376708_;
         this.effectDuration = p_377657_;
         this.longSwitchSound = p_376837_;
         this.shortSwitchSound = p_378282_;
         this.particleColor = p_375732_;
      }

      public Block block() {
         return this.open ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM;
      }

      public BlockState state() {
         return this.block().defaultBlockState();
      }

      public EyeblossomBlock.Type transform() {
         return fromBoolean(!this.open);
      }

      public boolean emitSounds() {
         return this.open;
      }

      public static EyeblossomBlock.Type fromBoolean(boolean p_376282_) {
         return p_376282_ ? OPEN : CLOSED;
      }

      public void spawnTransformParticle(ServerLevel p_377776_, BlockPos p_378624_, RandomSource p_375699_) {
         Vec3 vec3 = p_378624_.getCenter();
         double d0 = 0.5 + p_375699_.nextDouble();
         Vec3 vec31 = new Vec3(p_375699_.nextDouble() - 0.5, p_375699_.nextDouble() + 1.0, p_375699_.nextDouble() - 0.5);
         Vec3 vec32 = vec3.add(vec31.scale(d0));
         TrailParticleOption trailparticleoption = new TrailParticleOption(vec32, this.particleColor, (int)(20.0 * d0));
         p_377776_.sendParticles(trailparticleoption, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      public SoundEvent longSwitchSound() {
         return this.longSwitchSound;
      }
   }
}
