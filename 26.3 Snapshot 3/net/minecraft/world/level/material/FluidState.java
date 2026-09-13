package net.minecraft.world.level.material;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class FluidState extends StateHolder<Fluid, FluidState> implements TypedInstance<Fluid> {
   public static final Codec<FluidState> CODEC = codec(BuiltInRegistries.FLUID.byNameCodec(), Fluid::defaultFluidState, Fluid::getStateDefinition).stable();
   public static final int AMOUNT_MAX = 9;
   public static final int AMOUNT_FULL = 8;

   public FluidState(final Fluid owner, final Property<?>[] propertyKeys, final Comparable<?>[] propertyValues) {
      super(owner, propertyKeys, propertyValues);
   }

   public Fluid getType() {
      return this.owner;
   }

   public boolean isSource() {
      return this.getType().isSource(this);
   }

   public boolean isSourceOfType(final Fluid fluidType) {
      return this.owner == fluidType && this.owner.isSource(this);
   }

   public boolean isEmpty() {
      return this.getType().isEmpty();
   }

   public float getHeight(final BlockGetter level, final BlockPos pos) {
      return this.getType().getHeight(this, level, pos);
   }

   public float getOwnHeight() {
      return this.getType().getOwnHeight(this);
   }

   public boolean isFull() {
      return this.getAmount() == 8;
   }

   public int getAmount() {
      return this.getType().getAmount(this);
   }

   public boolean shouldRenderBackwardUpFace(final BlockGetter level, final BlockPos above) {
      for (int ox = -1; ox <= 1; ox++) {
         for (int oz = -1; oz <= 1; oz++) {
            BlockPos offset = above.offset(ox, 0, oz);
            FluidState fluidState = level.getFluidState(offset);
            if (!fluidState.getType().isSame(this.getType()) && !level.getBlockState(offset).isSolidRender()) {
               return true;
            }
         }
      }

      return false;
   }

   public void tick(final ServerLevel level, final BlockPos pos, final BlockState blockState) {
      this.getType().tick(level, pos, blockState, this);
   }

   public void animateTick(final Level level, final BlockPos pos, final RandomSource random) {
      this.getType().animateTick(level, pos, this, random);
   }

   public boolean isRandomlyTicking() {
      return this.getType().isRandomlyTicking();
   }

   public void randomTick(final ServerLevel level, final BlockPos pos, final RandomSource random) {
      this.getType().randomTick(level, pos, this, random);
   }

   public Vec3 getFlow(final BlockGetter level, final BlockPos pos) {
      return this.getType().getFlow(level, pos, this);
   }

   public BlockState createLegacyBlock() {
      return this.getType().createLegacyBlock(this);
   }

   public @Nullable ParticleOptions getDripParticle() {
      return this.getType().getDripParticle();
   }

   @Override
   public Holder<Fluid> typeHolder() {
      return this.getType().builtInRegistryHolder();
   }

   public float getExplosionResistance() {
      return this.getType().getExplosionResistance();
   }

   public boolean canBeReplacedWith(final BlockGetter level, final BlockPos pos, final Fluid other, final Direction direction) {
      return this.getType().canBeReplacedWith(this, level, pos, other, direction);
   }

   public VoxelShape getShape(final BlockGetter level, final BlockPos pos) {
      return this.getType().getShape(this, level, pos);
   }

   public @Nullable AABB getAABB(final BlockGetter level, final BlockPos pos) {
      return this.getType().getAABB(this, level, pos);
   }

   public void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
      this.getType().entityInside(level, pos, entity, effectApplier);
   }
}
