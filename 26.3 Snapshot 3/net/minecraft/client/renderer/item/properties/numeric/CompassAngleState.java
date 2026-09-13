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
import org.jspecify.annotations.Nullable;

public class CompassAngleState extends NeedleDirectionHelper {
   public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
            CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
         )
         .apply(i, CompassAngleState::new)
   );
   private final NeedleDirectionHelper.Wobbler wobbler;
   private final NeedleDirectionHelper.Wobbler noTargetWobbler;
   private final CompassAngleState.CompassTarget compassTarget;
   private final RandomSource random = RandomSource.create();

   public CompassAngleState(final boolean wobble, final CompassAngleState.CompassTarget compassTarget) {
      super(wobble);
      this.wobbler = this.newWobbler(0.8F);
      this.noTargetWobbler = this.newWobbler(0.8F);
      this.compassTarget = compassTarget;
   }

   @Override
   protected float calculate(final ItemStack itemStack, final ClientLevel level, final int seed, final ItemOwner owner) {
      GlobalPos compassTargetPos = this.compassTarget.get(level, itemStack, owner);
      long gameTime = level.getGameTime();
      return !isValidCompassTargetPos(owner, compassTargetPos)
         ? this.getRandomlySpinningRotation(seed, gameTime)
         : this.getRotationTowardsCompassTarget(owner, gameTime, compassTargetPos.pos());
   }

   private float getRandomlySpinningRotation(final int seed, final long gameTime) {
      if (this.noTargetWobbler.shouldUpdate(gameTime)) {
         this.noTargetWobbler.update(gameTime, this.random.nextFloat());
      }

      float targetRotation = this.noTargetWobbler.rotation() + hash(seed) / 2.1474836E9F;
      return Mth.positiveModulo(targetRotation, 1.0F);
   }

   private float getRotationTowardsCompassTarget(final ItemOwner owner, final long gameTime, final BlockPos compassTargetPos) {
      float angleToTarget = (float)getAngleFromEntityToPos(owner, compassTargetPos);
      float ownerYRotation = getWrappedVisualRotationY(owner);
      float targetRotation;
      if (owner.asLivingEntity() instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
         if (this.wobbler.shouldUpdate(gameTime)) {
            this.wobbler.update(gameTime, 0.5F - (ownerYRotation - 0.25F));
         }

         targetRotation = angleToTarget + this.wobbler.rotation();
      } else {
         targetRotation = 0.5F - (ownerYRotation - 0.25F - angleToTarget);
      }

      return Mth.positiveModulo(targetRotation, 1.0F);
   }

   private static boolean isValidCompassTargetPos(final ItemOwner owner, final @Nullable GlobalPos positionToPointTo) {
      return positionToPointTo != null
         && positionToPointTo.dimension() == owner.level().dimension()
         && !(positionToPointTo.pos().distToCenterSqr(owner.position()) < 1.0E-5F);
   }

   private static double getAngleFromEntityToPos(final ItemOwner owner, final BlockPos position) {
      Vec3 target = Vec3.atCenterOf(position);
      Vec3 ownerPosition = owner.position();
      return Math.atan2(target.z() - ownerPosition.z(), target.x() - ownerPosition.x()) / (float) (Math.PI * 2);
   }

   private static float getWrappedVisualRotationY(final ItemOwner owner) {
      return Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
   }

   private static int hash(final int input) {
      return input * 1327217883;
   }

   protected CompassAngleState.CompassTarget target() {
      return this.compassTarget;
   }

   public enum CompassTarget implements StringRepresentable {
      NONE("none") {
         @Override
         public @Nullable GlobalPos get(final ClientLevel level, final ItemStack itemStack, final @Nullable ItemOwner owner) {
            return null;
         }
      },
      LODESTONE("lodestone") {
         @Override
         public @Nullable GlobalPos get(final ClientLevel level, final ItemStack itemStack, final @Nullable ItemOwner owner) {
            LodestoneTracker tracker = itemStack.get(DataComponents.LODESTONE_TRACKER);
            return tracker != null ? tracker.target().orElse(null) : null;
         }
      },
      SPAWN("spawn") {
         @Override
         public GlobalPos get(final ClientLevel level, final ItemStack itemStack, final @Nullable ItemOwner owner) {
            return level.getRespawnData().globalPos();
         }
      },
      RECOVERY("recovery") {
         @Override
         public @Nullable GlobalPos get(final ClientLevel level, final ItemStack itemStack, final @Nullable ItemOwner owner) {
            return (owner == null ? null : owner.asLivingEntity()) instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
         }
      };

      public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
      private final String name;

      CompassTarget(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract @Nullable GlobalPos get(final ClientLevel level, final ItemStack itemStack, final @Nullable ItemOwner entity);
   }
}
