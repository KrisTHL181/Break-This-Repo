package net.minecraft.world.level.material;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class Fluid {
   public static final IdMapper<FluidState> FLUID_STATE_REGISTRY = new IdMapper<>();
   protected final StateDefinition<Fluid, FluidState> stateDefinition;
   private FluidState defaultFluidState;
   private final Holder.Reference<Fluid> builtInRegistryHolder = BuiltInRegistries.FLUID.createIntrusiveHolder(this);

   protected Fluid() {
      StateDefinition.Builder<Fluid, FluidState> builder = new StateDefinition.Builder<>(this);
      this.createFluidStateDefinition(builder);
      this.stateDefinition = builder.create(Fluid::defaultFluidState, FluidState::new);
      this.registerDefaultState(this.stateDefinition.any());
   }

   protected void createFluidStateDefinition(final StateDefinition.Builder<Fluid, FluidState> builder) {
   }

   public StateDefinition<Fluid, FluidState> getStateDefinition() {
      return this.stateDefinition;
   }

   protected final void registerDefaultState(final FluidState state) {
      this.defaultFluidState = state;
   }

   public final FluidState defaultFluidState() {
      return this.defaultFluidState;
   }

   public abstract Item getBucket();

   protected void animateTick(final Level level, final BlockPos pos, final FluidState fluidState, final RandomSource random) {
   }

   protected void tick(final ServerLevel level, final BlockPos pos, final BlockState blockState, final FluidState fluidState) {
   }

   protected void randomTick(final ServerLevel level, final BlockPos pos, final FluidState fluidState, final RandomSource random) {
   }

   protected void entityInside(final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier) {
   }

   protected @Nullable ParticleOptions getDripParticle() {
      return null;
   }

   protected abstract boolean canBeReplacedWith(FluidState state, final BlockGetter level, final BlockPos pos, Fluid other, Direction direction);

   protected abstract Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState);

   public abstract int getTickDelay(LevelReader level);

   protected boolean isRandomlyTicking() {
      return false;
   }

   protected boolean isEmpty() {
      return false;
   }

   protected abstract float getExplosionResistance();

   public abstract float getHeight(FluidState fluidState, final BlockGetter level, final BlockPos pos);

   public abstract float getOwnHeight(FluidState fluidState);

   protected abstract BlockState createLegacyBlock(FluidState fluidState);

   public abstract boolean isSource(FluidState fluidState);

   public abstract int getAmount(FluidState fluidState);

   public boolean isSame(final Fluid other) {
      return other == this;
   }

   @Deprecated
   public boolean is(final TagKey<Fluid> tag) {
      return this.builtInRegistryHolder.is(tag);
   }

   public abstract VoxelShape getShape(final FluidState state, final BlockGetter level, final BlockPos pos);

   public @Nullable AABB getAABB(final FluidState state, final BlockGetter level, final BlockPos pos) {
      if (this.isEmpty()) {
         return null;
      }

      float height = state.getHeight(level, pos);
      return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + height, pos.getZ() + 1.0);
   }

   public Optional<SoundEvent> getPickupSound() {
      return Optional.empty();
   }

   @Deprecated
   public Holder.Reference<Fluid> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }
}
