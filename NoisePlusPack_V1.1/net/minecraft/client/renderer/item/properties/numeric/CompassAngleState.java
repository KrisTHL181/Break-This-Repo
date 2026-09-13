package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompassAngleState extends NeedleDirectionHelper {
   public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377373_ -> p_377373_.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
            CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
         )
         .apply(p_377373_, CompassAngleState::new)
   );
   private final NeedleDirectionHelper.Wobbler wobbler;
   private final NeedleDirectionHelper.Wobbler noTargetWobbler;
   private final CompassAngleState.CompassTarget compassTarget;
   private final RandomSource random = RandomSource.create();

   public CompassAngleState(boolean p_375464_, CompassAngleState.CompassTarget p_375747_) {
      super(p_375464_);
      this.wobbler = this.newWobbler(0.8F);
      this.noTargetWobbler = this.newWobbler(0.8F);
      this.compassTarget = p_375747_;
   }

   @Override
   protected float calculate(ItemStack p_376712_, ClientLevel p_377258_, int p_377034_, ItemOwner p_425659_) {
      GlobalPos globalpos = this.compassTarget.get(p_377258_, p_376712_, p_425659_);
      long i = p_377258_.getGameTime();
      return !isValidCompassTargetPos(p_425659_, globalpos)
         ? this.getRandomlySpinningRotation(p_377034_, i)
         : this.getRotationTowardsCompassTarget(p_425659_, i, globalpos.pos());
   }

   private float getRandomlySpinningRotation(int p_375455_, long p_378047_) {
      if (this.noTargetWobbler.shouldUpdate(p_378047_)) {
         this.noTargetWobbler.update(p_378047_, this.random.nextFloat());
      }

      float f = this.noTargetWobbler.rotation() + hash(p_375455_) / 2.1474836E9F;
      return Mth.positiveModulo(f, 1.0F);
   }

   private float getRotationTowardsCompassTarget(ItemOwner p_429353_, long p_375437_, BlockPos p_376106_) {
      float f = (float)getAngleFromEntityToPos(p_429353_, p_376106_);
      float f1 = getWrappedVisualRotationY(p_429353_);
      float f2;
      if (p_429353_.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
         if (this.wobbler.shouldUpdate(p_375437_)) {
            this.wobbler.update(p_375437_, 0.5F - (f1 - 0.25F));
         }

         f2 = f + this.wobbler.rotation();
      } else {
         f2 = 0.5F - (f1 - 0.25F - f);
      }

      return Mth.positiveModulo(f2, 1.0F);
   }

   private static boolean isValidCompassTargetPos(ItemOwner p_423243_, @Nullable GlobalPos p_376149_) {
      return p_376149_ != null && p_376149_.dimension() == p_423243_.level().dimension() && !(p_376149_.pos().distToCenterSqr(p_423243_.position()) < 1.0E-5F);
   }

   private static double getAngleFromEntityToPos(ItemOwner p_428641_, BlockPos p_375957_) {
      Vec3 vec3 = Vec3.atCenterOf(p_375957_);
      Vec3 vec31 = p_428641_.position();
      return Math.atan2(vec3.z() - vec31.z(), vec3.x() - vec31.x()) / (float) (Math.PI * 2);
   }

   private static float getWrappedVisualRotationY(ItemOwner p_427888_) {
      return Mth.positiveModulo(p_427888_.getVisualRotationYInDegrees() / 360.0F, 1.0F);
   }

   private static int hash(int p_376466_) {
      return p_376466_ * 1327217883;
   }

   protected CompassAngleState.CompassTarget target() {
      return this.compassTarget;
   }

   @OnlyIn(Dist.CLIENT)
   public enum CompassTarget implements StringRepresentable {
      NONE("none") {
         @Override
         public @Nullable GlobalPos get(ClientLevel p_375914_, ItemStack p_376028_, @Nullable ItemOwner p_423750_) {
            return null;
         }
      },
      LODESTONE("lodestone") {
         @Override
         public @Nullable GlobalPos get(ClientLevel p_376563_, ItemStack p_377548_, @Nullable ItemOwner p_423946_) {
            LodestoneTracker lodestonetracker = p_377548_.get(DataComponents.LODESTONE_TRACKER);
            return lodestonetracker != null ? lodestonetracker.target().orElse(null) : null;
         }
      },
      SPAWN("spawn") {
         @Override
         public GlobalPos get(ClientLevel p_378146_, ItemStack p_377238_, @Nullable ItemOwner p_425022_) {
            return p_378146_.getRespawnData().globalPos();
         }
      },
      RECOVERY("recovery") {
         @Override
         public @Nullable GlobalPos get(ClientLevel p_375618_, ItemStack p_378046_, @Nullable ItemOwner p_425437_) {
            return (p_425437_ == null ? null : p_425437_.asLivingEntity()) instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
         }
      };

      public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
      private final String name;

      CompassTarget(final String p_376851_) {
         this.name = p_376851_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      abstract @Nullable GlobalPos get(ClientLevel var1, ItemStack var2, @Nullable ItemOwner var3);
   }
}
