package net.minecraft.world.entity.decoration.painting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Painting extends HangingEntity {
   private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(
      Painting.class, EntityDataSerializers.PAINTING_VARIANT
   );
   public static final float DEPTH = 0.0625F;

   public Painting(EntityType<? extends Painting> p_451242_, Level p_460839_) {
      super(p_451242_, p_460839_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454089_) {
      super.defineSynchedData(p_454089_);
      p_454089_.define(DATA_PAINTING_VARIANT_ID, VariantUtils.getAny(this.registryAccess(), Registries.PAINTING_VARIANT));
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_450885_) {
      super.onSyncedDataUpdated(p_450885_);
      if (DATA_PAINTING_VARIANT_ID.equals(p_450885_)) {
         this.recalculateBoundingBox();
      }
   }

   private void setVariant(Holder<PaintingVariant> p_458784_) {
      this.entityData.set(DATA_PAINTING_VARIANT_ID, p_458784_);
   }

   public Holder<PaintingVariant> getVariant() {
      return this.entityData.get(DATA_PAINTING_VARIANT_ID);
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_450282_) {
      return p_450282_ == DataComponents.PAINTING_VARIANT ? castComponentValue((DataComponentType<T>)p_450282_, this.getVariant()) : super.get(p_450282_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_452976_) {
      this.applyImplicitComponentIfPresent(p_452976_, DataComponents.PAINTING_VARIANT);
      super.applyImplicitComponents(p_452976_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_453992_, T p_451942_) {
      if (p_453992_ == DataComponents.PAINTING_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.PAINTING_VARIANT, p_451942_));
         return true;
      } else {
         return super.applyImplicitComponent(p_453992_, p_451942_);
      }
   }

   public static Optional<Painting> create(Level p_455052_, BlockPos p_457512_, Direction p_458844_) {
      Painting painting = new Painting(p_455052_, p_457512_);
      List<Holder<PaintingVariant>> list = new ArrayList<>();
      p_455052_.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach(list::add);
      if (list.isEmpty()) {
         return Optional.empty();
      }

      painting.setDirection(p_458844_);
      list.removeIf(p_451149_ -> {
         painting.setVariant((Holder<PaintingVariant>)p_451149_);
         return !painting.survives();
      });
      if (list.isEmpty()) {
         return Optional.empty();
      }

      int i = list.stream().mapToInt(Painting::variantArea).max().orElse(0);
      list.removeIf(p_458547_ -> variantArea((Holder<PaintingVariant>)p_458547_) < i);
      Optional<Holder<PaintingVariant>> optional = Util.getRandomSafe(list, painting.random);
      if (optional.isEmpty()) {
         return Optional.empty();
      }

      painting.setVariant(optional.get());
      painting.setDirection(p_458844_);
      return Optional.of(painting);
   }

   private static int variantArea(Holder<PaintingVariant> p_452924_) {
      return p_452924_.value().area();
   }

   private Painting(Level p_450410_, BlockPos p_459258_) {
      super(EntityType.PAINTING, p_450410_, p_459258_);
   }

   public Painting(Level p_456398_, BlockPos p_457533_, Direction p_454208_, Holder<PaintingVariant> p_460665_) {
      this(p_456398_, p_457533_);
      this.setVariant(p_460665_);
      this.setDirection(p_454208_);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_450817_) {
      p_450817_.store("facing", Direction.LEGACY_ID_CODEC_2D, this.getDirection());
      super.addAdditionalSaveData(p_450817_);
      VariantUtils.writeVariant(p_450817_, this.getVariant());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_457787_) {
      Direction direction = p_457787_.<Direction>read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
      super.readAdditionalSaveData(p_457787_);
      this.setDirection(direction);
      VariantUtils.readVariant(p_457787_, Registries.PAINTING_VARIANT).ifPresent(this::setVariant);
   }

   @Override
   protected AABB calculateBoundingBox(BlockPos p_455934_, Direction p_450849_) {
      float f = 0.46875F;
      Vec3 vec3 = Vec3.atCenterOf(p_455934_).relative(p_450849_, -0.46875);
      PaintingVariant paintingvariant = this.getVariant().value();
      double d0 = this.offsetForPaintingSize(paintingvariant.width());
      double d1 = this.offsetForPaintingSize(paintingvariant.height());
      Direction direction = p_450849_.getCounterClockWise();
      Vec3 vec31 = vec3.relative(direction, d0).relative(Direction.UP, d1);
      Direction.Axis direction$axis = p_450849_.getAxis();
      double d2 = direction$axis == Direction.Axis.X ? 0.0625 : paintingvariant.width();
      double d3 = paintingvariant.height();
      double d4 = direction$axis == Direction.Axis.Z ? 0.0625 : paintingvariant.width();
      return AABB.ofSize(vec31, d2, d3, d4);
   }

   private double offsetForPaintingSize(int p_455668_) {
      return p_455668_ % 2 == 0 ? 0.5 : 0.0;
   }

   @Override
   public void dropItem(ServerLevel p_458587_, @Nullable Entity p_454943_) {
      if (p_458587_.getGameRules().get(GameRules.ENTITY_DROPS)) {
         this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
         if (!(p_454943_ instanceof Player player && player.hasInfiniteMaterials())) {
            this.spawnAtLocation(p_458587_, Items.PAINTING);
         }
      }
   }

   @Override
   public void playPlacementSound() {
      this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
   }

   @Override
   public void snapTo(double p_459893_, double p_451184_, double p_451882_, float p_456939_, float p_451017_) {
      this.setPos(p_459893_, p_451184_, p_451882_);
   }

   @Override
   public Vec3 trackingPosition() {
      return Vec3.atLowerCornerOf(this.pos);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_459562_) {
      return new ClientboundAddEntityPacket(this, this.getDirection().get3DDataValue(), this.getPos());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_460617_) {
      super.recreateFromPacket(p_460617_);
      this.setDirection(Direction.from3DDataValue(p_460617_.getData()));
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.PAINTING);
   }
}
