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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
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
   private static final String TAG_STEW_EFFECTS = "stew_effects";
   private @Nullable SuspiciousStewEffects stewEffects;
   private @Nullable UUID lastLightningBoltUUID;

   public MushroomCow(EntityType<? extends MushroomCow> p_459154_, Level p_458567_) {
      super(p_459154_, p_458567_);
   }

   @Override
   public float getWalkTargetValue(BlockPos p_455818_, LevelReader p_455238_) {
      return p_455238_.getBlockState(p_455818_.below()).is(Blocks.MYCELIUM) ? 10.0F : p_455238_.getPathfindingCostFromLightLevels(p_455818_);
   }

   public static boolean checkMushroomSpawnRules(
      EntityType<MushroomCow> p_452376_, LevelAccessor p_453400_, EntitySpawnReason p_459628_, BlockPos p_451279_, RandomSource p_460174_
   ) {
      return p_453400_.getBlockState(p_451279_.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(p_453400_, p_451279_);
   }

   @Override
   public void thunderHit(ServerLevel p_450942_, LightningBolt p_455463_) {
      UUID uuid = p_455463_.getUUID();
      if (!uuid.equals(this.lastLightningBoltUUID)) {
         this.setVariant(this.getVariant() == MushroomCow.Variant.RED ? MushroomCow.Variant.BROWN : MushroomCow.Variant.RED);
         this.lastLightningBoltUUID = uuid;
         this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_451836_) {
      super.defineSynchedData(p_451836_);
      p_451836_.define(DATA_TYPE, MushroomCow.Variant.DEFAULT.id);
   }

   @Override
   public InteractionResult mobInteract(Player p_454203_, InteractionHand p_457673_) {
      ItemStack itemstack = p_454203_.getItemInHand(p_457673_);
      if (itemstack.is(Items.BOWL) && !this.isBaby()) {
         boolean flag = false;
         ItemStack itemstack1;
         if (this.stewEffects != null) {
            flag = true;
            itemstack1 = new ItemStack(Items.SUSPICIOUS_STEW);
            itemstack1.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
            this.stewEffects = null;
         } else {
            itemstack1 = new ItemStack(Items.MUSHROOM_STEW);
         }

         ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, p_454203_, itemstack1, false);
         p_454203_.setItemInHand(p_457673_, itemstack2);
         SoundEvent soundevent;
         if (flag) {
            soundevent = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
         } else {
            soundevent = SoundEvents.MOOSHROOM_MILK;
         }

         this.playSound(soundevent, 1.0F, 1.0F);
         return InteractionResult.SUCCESS;
      } else if (itemstack.is(Items.SHEARS) && this.readyForShearing()) {
         if (this.level() instanceof ServerLevel serverlevel) {
            this.shear(serverlevel, SoundSource.PLAYERS, itemstack);
            this.gameEvent(GameEvent.SHEAR, p_454203_);
            itemstack.hurtAndBreak(1, p_454203_, p_457673_.asEquipmentSlot());
         }

         return InteractionResult.SUCCESS;
      } else if (this.getVariant() == MushroomCow.Variant.BROWN) {
         Optional<SuspiciousStewEffects> optional = this.getEffectsFromItemStack(itemstack);
         if (optional.isEmpty()) {
            return super.mobInteract(p_454203_, p_457673_);
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
            itemstack.consume(1, p_454203_);
            SpellParticleOption spellparticleoption = SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0F);

            for (int j = 0; j < 4; j++) {
               this.level()
                  .addParticle(
                     spellparticleoption,
                     this.getX() + this.random.nextDouble() / 2.0,
                     this.getY(0.5),
                     this.getZ() + this.random.nextDouble() / 2.0,
                     0.0,
                     this.random.nextDouble() / 5.0,
                     0.0
                  );
            }

            this.stewEffects = optional.get();
            this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(p_454203_, p_457673_);
      }
   }

   @Override
   public void shear(ServerLevel p_454992_, SoundSource p_460786_, ItemStack p_454795_) {
      p_454992_.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, p_460786_, 1.0F, 1.0F);
      this.convertTo(EntityType.COW, ConversionParams.single(this, false, false), p_458024_ -> {
         p_454992_.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         this.dropFromShearingLootTable(p_454992_, BuiltInLootTables.SHEAR_MOOSHROOM, p_454795_, (p_460709_, p_451895_) -> {
            for (int i = 0; i < p_451895_.getCount(); i++) {
               p_460709_.addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0), this.getZ(), p_451895_.copyWithCount(1)));
            }
         });
      });
   }

   @Override
   public boolean readyForShearing() {
      return this.isAlive() && !this.isBaby();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_455928_) {
      super.addAdditionalSaveData(p_455928_);
      p_455928_.store("Type", MushroomCow.Variant.CODEC, this.getVariant());
      p_455928_.storeNullable("stew_effects", SuspiciousStewEffects.CODEC, this.stewEffects);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_453974_) {
      super.readAdditionalSaveData(p_453974_);
      this.setVariant(p_453974_.<MushroomCow.Variant>read("Type", MushroomCow.Variant.CODEC).orElse(MushroomCow.Variant.DEFAULT));
      this.stewEffects = p_453974_.<SuspiciousStewEffects>read("stew_effects", SuspiciousStewEffects.CODEC).orElse(null);
   }

   private Optional<SuspiciousStewEffects> getEffectsFromItemStack(ItemStack p_458897_) {
      SuspiciousEffectHolder suspiciouseffectholder = SuspiciousEffectHolder.tryGet(p_458897_.getItem());
      return suspiciouseffectholder != null ? Optional.of(suspiciouseffectholder.getSuspiciousEffects()) : Optional.empty();
   }

   private void setVariant(MushroomCow.Variant p_458325_) {
      this.entityData.set(DATA_TYPE, p_458325_.id);
   }

   public MushroomCow.Variant getVariant() {
      return MushroomCow.Variant.byId(this.entityData.get(DATA_TYPE));
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_454953_) {
      return p_454953_ == DataComponents.MOOSHROOM_VARIANT ? castComponentValue((DataComponentType<T>)p_454953_, this.getVariant()) : super.get(p_454953_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_455032_) {
      this.applyImplicitComponentIfPresent(p_455032_, DataComponents.MOOSHROOM_VARIANT);
      super.applyImplicitComponents(p_455032_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_450926_, T p_450646_) {
      if (p_450926_ == DataComponents.MOOSHROOM_VARIANT) {
         this.setVariant(castComponentValue(DataComponents.MOOSHROOM_VARIANT, p_450646_));
         return true;
      } else {
         return super.applyImplicitComponent(p_450926_, p_450646_);
      }
   }

   public @Nullable MushroomCow getBreedOffspring(ServerLevel p_454117_, AgeableMob p_458786_) {
      MushroomCow mushroomcow = EntityType.MOOSHROOM.create(p_454117_, EntitySpawnReason.BREEDING);
      if (mushroomcow != null) {
         mushroomcow.setVariant(this.getOffspringVariant((MushroomCow)p_458786_));
      }

      return mushroomcow;
   }

   private MushroomCow.Variant getOffspringVariant(MushroomCow p_459581_) {
      MushroomCow.Variant mushroomcow$variant = this.getVariant();
      MushroomCow.Variant mushroomcow$variant1 = p_459581_.getVariant();
      MushroomCow.Variant mushroomcow$variant2;
      if (mushroomcow$variant == mushroomcow$variant1 && this.random.nextInt(1024) == 0) {
         mushroomcow$variant2 = mushroomcow$variant == MushroomCow.Variant.BROWN ? MushroomCow.Variant.RED : MushroomCow.Variant.BROWN;
      } else {
         mushroomcow$variant2 = this.random.nextBoolean() ? mushroomcow$variant : mushroomcow$variant1;
      }

      return mushroomcow$variant2;
   }

   public enum Variant implements StringRepresentable {
      RED("red", 0, Blocks.RED_MUSHROOM.defaultBlockState()),
      BROWN("brown", 1, Blocks.BROWN_MUSHROOM.defaultBlockState());

      public static final MushroomCow.Variant DEFAULT = RED;
      public static final Codec<MushroomCow.Variant> CODEC = StringRepresentable.fromEnum(MushroomCow.Variant::values);
      private static final IntFunction<MushroomCow.Variant> BY_ID = ByIdMap.continuous(MushroomCow.Variant::id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
      public static final StreamCodec<ByteBuf, MushroomCow.Variant> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, MushroomCow.Variant::id);
      private final String type;
      final int id;
      private final BlockState blockState;

      Variant(final String p_452372_, final int p_457866_, final BlockState p_451253_) {
         this.type = p_452372_;
         this.id = p_457866_;
         this.blockState = p_451253_;
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

      static MushroomCow.Variant byId(int p_458342_) {
         return BY_ID.apply(p_458342_);
      }
   }
}
