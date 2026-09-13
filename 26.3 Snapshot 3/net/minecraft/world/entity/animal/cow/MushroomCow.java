package net.minecraft.world.entity.animal.cow;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class MushroomCow extends AbstractCow implements Shearable {
   private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.INT);
   private static final int MUTATE_CHANCE = 1024;
   private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45F, 0.7F)
      .withEyeHeight(0.69F)
      .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, 0.75F, 0.0F));
   private static final String TAG_STEW_EFFECTS = "stew_effects";
   private @Nullable SuspiciousStewEffects stewEffects;
   private @Nullable UUID lastLightningBoltUUID;

   public MushroomCow(final EntityType<? extends MushroomCow> type, final Level level) {
      super(type, level);
   }

   @Override
   public float getWalkTargetValue(final BlockPos pos, final LevelReader level) {
      return level.getBlockState(pos.below()).is(Blocks.MYCELIUM) ? 10.0F : level.getPathfindingCostFromLightLevels(pos);
   }

   public static boolean checkMushroomSpawnRules(
      final EntityType<MushroomCow> type, final LevelAccessor level, final EntitySpawnReason spawnReason, final BlockPos pos, final RandomSource random
   ) {
      return level.getBlockState(pos.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
   }

   @Override
   public void thunderHit(final ServerLevel level, final LightningBolt lightningBolt) {
      UUID lightningBoltUUID = lightningBolt.getUUID();
      if (!lightningBoltUUID.equals(this.lastLightningBoltUUID)) {
         this.setVariant(this.getVariant() == MushroomCow.Variant.RED ? MushroomCow.Variant.BROWN : MushroomCow.Variant.RED);
         this.lastLightningBoltUUID = lightningBoltUUID;
         this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
      }
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_TYPE, MushroomCow.Variant.DEFAULT.id);
   }

   @Override
   public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (itemStack.is(Items.BOWL) && !this.isBaby()) {
         boolean isSuspicious = false;
         ItemStack stew;
         if (this.stewEffects != null) {
            isSuspicious = true;
            stew = new ItemStack(Items.SUSPICIOUS_STEW);
            stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
            this.stewEffects = null;
         } else {
            stew = new ItemStack(Items.MUSHROOM_STEW);
         }

         ItemStack bowlOrStew = ItemUtils.createFilledResult(itemStack, player, stew, false);
         player.setItemInHand(hand, bowlOrStew);
         SoundEvent milkSound;
         if (isSuspicious) {
            milkSound = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
         } else {
            milkSound = SoundEvents.MOOSHROOM_MILK;
         }

         this.playSound(milkSound, 1.0F, 1.0F);
         return InteractionResult.SUCCESS;
      } else if (itemStack.is(Items.SHEARS) && this.readyForShearing()) {
         if (this.level() instanceof ServerLevel level) {
            this.shear(level, SoundSource.PLAYERS, itemStack);
            this.gameEvent(GameEvent.SHEAR, player);
            itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else if (this.getVariant() == MushroomCow.Variant.BROWN && !this.isBaby()) {
         Optional<SuspiciousStewEffects> effectsFromItemStack = this.getEffectsFromItemStack(itemStack);
         if (effectsFromItemStack.isEmpty()) {
            return super.mobInteract(player, hand);
         }

         if (this.stewEffects != null) {
            for (int i = 0; i < 2; i++) {
               this.level()
                  .addParticle(
                     ParticleTypes.SMOKE,
                     this.getX() + this.random.nextDouble() / 2.0,
                     this.getY(0.5),
                     this.getZ() + this.random.nextDouble() / 2.0,
                     0.0,
                     this.random.nextDouble() / 5.0,
                     0.0
                  );
            }
         } else {
            itemStack.consume(1, player);
            SpellParticleOption particle = SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0F);

            for (int i = 0; i < 4; i++) {
               this.level()
                  .addParticle(
                     particle,
                     this.getX() + this.random.nextDouble() / 2.0,
                     this.getY(0.5),
                     this.getZ() + this.random.nextDouble() / 2.0,
                     0.0,
                     this.random.nextDouble() / 5.0,
                     0.0
                  );
            }

            this.stewEffects = effectsFromItemStack.get();
            this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   @Override
   public void shear(final ServerLevel level, final SoundSource soundSource, final ItemStack tool) {
      level.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, soundSource, 1.0F, 1.0F);
      this.convertTo(EntityTypes.COW, ConversionParams.single(this, false, false), cow -> {
         level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_MOOSHROOM, tool, (l, drop) -> {
            for (int i = 0; i < drop.getCount(); i++) {
               l.addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0), this.getZ(), drop.copyWithCount(1)));
            }
         });
      });
   }

   @Override
   public boolean readyForShearing() {
      return !this.isBaby();
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("Type", MushroomCow.Variant.CODEC, this.getVariant());
      output.storeNullable("stew_effects", SuspiciousStewEffects.CODEC, this.stewEffects);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setVariant(input.<MushroomCow.Variant>read("Type", MushroomCow.Variant.CODEC).orElse(MushroomCow.Variant.DEFAULT));
      this.stewEffects = input.<SuspiciousStewEffects>read("stew_effects", SuspiciousStewEffects.CODEC).orElse(null);
   }

   private Optional<SuspiciousStewEffects> getEffectsFromItemStack(final ItemStack itemStack) {
      SuspiciousEffectHolder effectHolder = SuspiciousEffectHolder.tryGet(itemStack.getItem());
      return effectHolder != null ? Optional.of(effectHolder.getSuspiciousEffects()) : Optional.empty();
   }

   private void setVariant(final MushroomCow.Variant variant) {
      this.entityData.set(DATA_TYPE, variant.id);
   }

   public MushroomCow.Variant getVariant() {
      return MushroomCow.Variant.byId(this.entityData.get(DATA_TYPE));
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      return type == DataComponents.MOOSHROOM_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getVariant()) : super.get(type);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.MOOSHROOM_VARIANT);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.MOOSHROOM_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.MOOSHROOM_VARIANT, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   @Override
   public EntityDimensions getDefaultDimensions(final Pose pose) {
      return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
   }

   public @Nullable MushroomCow getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      MushroomCow baby = EntityTypes.MOOSHROOM.create(level, EntitySpawnReason.BREEDING);
      if (baby != null) {
         baby.setVariant(this.getOffspringVariant((MushroomCow)partner));
      }

      return baby;
   }

   private MushroomCow.Variant getOffspringVariant(final MushroomCow mate) {
      MushroomCow.Variant variant = this.getVariant();
      MushroomCow.Variant mateVariant = mate.getVariant();
      MushroomCow.Variant babyVariant;
      if (variant == mateVariant && this.random.nextInt(1024) == 0) {
         babyVariant = variant == MushroomCow.Variant.BROWN ? MushroomCow.Variant.RED : MushroomCow.Variant.BROWN;
      } else {
         babyVariant = this.random.nextBoolean() ? variant : mateVariant;
      }

      return babyVariant;
   }

   public enum Variant implements StringRepresentable {
      RED("red", 0, Blocks.RED_MUSHROOM.defaultBlockState()),
      BROWN("brown", 1, Blocks.BROWN_MUSHROOM.defaultBlockState());

      public static final MushroomCow.Variant DEFAULT = RED;
      public static final Codec<MushroomCow.Variant> CODEC = StringRepresentable.fromEnum(MushroomCow.Variant::values);
      private static final IntFunction<MushroomCow.Variant> BY_ID = ByIdMap.continuous(MushroomCow.Variant::id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final StreamCodec<ByteBuf, MushroomCow.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, MushroomCow.Variant::id);
      private final String type;
      private final int id;
      private final BlockState blockState;

      Variant(final String type, final int id, final BlockState blockState) {
         this.type = type;
         this.id = id;
         this.blockState = blockState;
      }

      public BlockState getBlockState() {
         return this.blockState;
      }

      @Override
      public String getSerializedName() {
         return this.type;
      }

      private int id() {
         return this.id;
      }

      private static MushroomCow.Variant byId(final int id) {
         return BY_ID.apply(id);
      }
   }
}
