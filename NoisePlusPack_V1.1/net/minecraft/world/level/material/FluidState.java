package net.minecraft.world.level.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
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

public final class FluidState extends StateHolder<Fluid, FluidState> {
   public static final Codec<FluidState> CODEC = codec(BuiltInRegistries.FLUID.byNameCodec(), Fluid::defaultFluidState).stable();
   public static final int AMOUNT_MAX = 9;
   public static final int AMOUNT_FULL = 8;

   public FluidState(Fluid p_76149_, Reference2ObjectArrayMap<Property<?>, Comparable<?>> p_332108_, MapCodec<FluidState> p_76151_) {
      super(p_76149_, p_332108_, p_76151_);
   }

   public Fluid getType() {
      return this.owner;
   }

   public boolean isSource() {
      return this.getType().isSource(this);
   }

   public boolean isSourceOfType(Fluid p_164513_) {
      return this.owner == p_164513_ && this.owner.isSource(this);
   }

   public boolean isEmpty() {
      return this.getType().isEmpty();
   }

   public float getHeight(BlockGetter p_76156_, BlockPos p_76157_) {
      return this.getType().getHeight(this, p_76156_, p_76157_);
   }

   public float getOwnHeight() {
      return this.getType().getOwnHeight(this);
   }

   public int getAmount() {
      return this.getType().getAmount(this);
   }

   public boolean shouldRenderBackwardUpFace(BlockGetter p_76172_, BlockPos p_76173_) {
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            BlockPos blockpos = p_76173_.offset(i, 0, j);
            FluidState fluidstate = p_76172_.getFluidState(blockpos);
            if (!fluidstate.getType().isSame(this.getType()) && !p_76172_.getBlockState(blockpos).isSolidRender()) {
               return true;
            }
         }
      }

      return false;
   }

   public void tick(ServerLevel p_366366_, BlockPos p_76165_, BlockState p_361746_) {
      this.getType().tick(p_366366_, p_76165_, p_361746_, this);
   }

   public void animateTick(Level p_230559_, BlockPos p_230560_, RandomSource p_230561_) {
      this.getType().animateTick(p_230559_, p_230560_, this, p_230561_);
   }

   public boolean isRandomlyTicking() {
      return this.getType().isRandomlyTicking();
   }

   public void randomTick(ServerLevel p_366389_, BlockPos p_230564_, RandomSource p_230565_) {
      this.getType().randomTick(p_366389_, p_230564_, this, p_230565_);
   }

   public Vec3 getFlow(BlockGetter p_76180_, BlockPos p_76181_) {
      return this.getType().getFlow(p_76180_, p_76181_, this);
   }

   public BlockState createLegacyBlock() {
      return this.getType().createLegacyBlock(this);
   }

   public @Nullable ParticleOptions getDripParticle() {
      return this.getType().getDripParticle();
   }

   public boolean is(TagKey<Fluid> p_205071_) {
      return this.getType().builtInRegistryHolder().is(p_205071_);
   }

   public boolean is(HolderSet<Fluid> p_205073_) {
      return p_205073_.contains(this.getType().builtInRegistryHolder());
   }

   public boolean is(Fluid p_192918_) {
      return this.getType() == p_192918_;
   }

   public float getExplosionResistance() {
      return this.getType().getExplosionResistance();
   }

   public boolean canBeReplacedWith(BlockGetter p_76159_, BlockPos p_76160_, Fluid p_76161_, Direction p_76162_) {
      return this.getType().canBeReplacedWith(this, p_76159_, p_76160_, p_76161_, p_76162_);
   }

   public VoxelShape getShape(BlockGetter p_76184_, BlockPos p_76185_) {
      return this.getType().getShape(this, p_76184_, p_76185_);
   }

   public @Nullable AABB getAABB(BlockGetter p_394145_, BlockPos p_395008_) {
      return this.getType().getAABB(this, p_394145_, p_395008_);
   }

   public Holder<Fluid> holder() {
      return this.owner.builtInRegistryHolder();
   }

   public Stream<TagKey<Fluid>> getTags() {
      return this.owner.builtInRegistryHolder().tags();
   }

   public void entityInside(Level p_392024_, BlockPos p_395365_, Entity p_394868_, InsideBlockEffectApplier p_396208_) {
      this.getType().entityInside(p_392024_, p_395365_, p_394868_, p_396208_);
   }
}
