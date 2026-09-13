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

   protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> p_76121_) {
   }

   public StateDefinition<Fluid, FluidState> getStateDefinition() {
      return this.stateDefinition;
   }

   protected final void registerDefaultState(FluidState p_76143_) {
      this.defaultFluidState = p_76143_;
   }

   public final FluidState defaultFluidState() {
      return this.defaultFluidState;
   }

   public abstract Item getBucket();

   protected void animateTick(Level p_230550_, BlockPos p_230551_, FluidState p_230552_, RandomSource p_230553_) {
   }

   protected void tick(ServerLevel p_369347_, BlockPos p_76114_, BlockState p_361596_, FluidState p_76115_) {
   }

   protected void randomTick(ServerLevel p_363339_, BlockPos p_230555_, FluidState p_230556_, RandomSource p_230557_) {
   }

   protected void entityInside(Level p_394813_, BlockPos p_394655_, Entity p_396881_, InsideBlockEffectApplier p_395108_) {
   }

   protected @Nullable ParticleOptions getDripParticle() {
      return null;
   }

   protected abstract boolean canBeReplacedWith(FluidState var1, BlockGetter var2, BlockPos var3, Fluid var4, Direction var5);

   protected abstract Vec3 getFlow(BlockGetter var1, BlockPos var2, FluidState var3);

   public abstract int getTickDelay(LevelReader var1);

   protected boolean isRandomlyTicking() {
      return false;
   }

   protected boolean isEmpty() {
      return false;
   }

   protected abstract float getExplosionResistance();

   public abstract float getHeight(FluidState var1, BlockGetter var2, BlockPos var3);

   public abstract float getOwnHeight(FluidState var1);

   protected abstract BlockState createLegacyBlock(FluidState var1);

   public abstract boolean isSource(FluidState var1);

   public abstract int getAmount(FluidState var1);

   public boolean isSame(Fluid p_76122_) {
      return p_76122_ == this;
   }

   @Deprecated
   public boolean is(TagKey<Fluid> p_205068_) {
      return this.builtInRegistryHolder.is(p_205068_);
   }

   public abstract VoxelShape getShape(FluidState var1, BlockGetter var2, BlockPos var3);

   public @Nullable AABB getAABB(FluidState p_393112_, BlockGetter p_397063_, BlockPos p_393864_) {
      if (this.isEmpty()) {
         return null;
      }

      float f = p_393112_.getHeight(p_397063_, p_393864_);
      return new AABB(p_393864_.getX(), p_393864_.getY(), p_393864_.getZ(), p_393864_.getX() + 1.0, p_393864_.getY() + f, p_393864_.getZ() + 1.0);
   }

   public Optional<SoundEvent> getPickupSound() {
      return Optional.empty();
   }

   @Deprecated
   public Holder.Reference<Fluid> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }
}
