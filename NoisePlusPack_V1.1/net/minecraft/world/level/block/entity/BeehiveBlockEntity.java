package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BeehiveBlockEntity extends BlockEntity {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_FLOWER_POS = "flower_pos";
   private static final String BEES = "bees";
   static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
      "Air",
      "drop_chances",
      "equipment",
      "Brain",
      "CanPickUpLoot",
      "DeathTime",
      "fall_distance",
      "FallFlying",
      "Fire",
      "HurtByTimestamp",
      "HurtTime",
      "LeftHanded",
      "Motion",
      "NoGravity",
      "OnGround",
      "PortalCooldown",
      "Pos",
      "Rotation",
      "sleeping_pos",
      "CannotEnterHiveTicks",
      "TicksSincePollination",
      "CropsGrownSincePollination",
      "hive_pos",
      "Passengers",
      "leash",
      "UUID"
   );
   public static final int MAX_OCCUPANTS = 3;
   private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
   private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
   public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
   private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
   private @Nullable BlockPos savedFlowerPos;

   public BeehiveBlockEntity(BlockPos p_155134_, BlockState p_155135_) {
      super(BlockEntityType.BEEHIVE, p_155134_, p_155135_);
   }

   @Override
   public void setChanged() {
      if (this.isFireNearby()) {
         this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      super.setChanged();
   }

   public boolean isFireNearby() {
      if (this.level == null) {
         return false;
      }

      for (BlockPos blockpos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
         if (this.level.getBlockState(blockpos).getBlock() instanceof FireBlock) {
            return true;
         }
      }

      return false;
   }

   public boolean isEmpty() {
      return this.stored.isEmpty();
   }

   public boolean isFull() {
      return this.stored.size() == 3;
   }

   public void emptyAllLivingFromHive(@Nullable Player p_58749_, BlockState p_58750_, BeehiveBlockEntity.BeeReleaseStatus p_58751_) {
      List<Entity> list = this.releaseAllOccupants(p_58750_, p_58751_);
      if (p_58749_ != null) {
         for (Entity entity : list) {
            if (entity instanceof Bee bee && p_58749_.position().distanceToSqr(entity.position()) <= 16.0) {
               if (!this.isSedated()) {
                  bee.setTarget(p_58749_);
               } else {
                  bee.setStayOutOfHiveCountdown(400);
               }
            }
         }
      }
   }

   private List<Entity> releaseAllOccupants(BlockState p_58760_, BeehiveBlockEntity.BeeReleaseStatus p_58761_) {
      List<Entity> list = Lists.newArrayList();
      this.stored.removeIf(p_327282_ -> releaseOccupant(this.level, this.worldPosition, p_58760_, p_327282_.toOccupant(), list, p_58761_, this.savedFlowerPos));
      if (!list.isEmpty()) {
         super.setChanged();
      }

      return list;
   }

   @VisibleForDebug
   public int getOccupantCount() {
      return this.stored.size();
   }

   public static int getHoneyLevel(BlockState p_58753_) {
      return p_58753_.getValue(BeehiveBlock.HONEY_LEVEL);
   }

   @VisibleForDebug
   public boolean isSedated() {
      return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
   }

   public void addOccupant(Bee p_454988_) {
      if (this.stored.size() < 3) {
         p_454988_.stopRiding();
         p_454988_.ejectPassengers();
         p_454988_.dropLeash();
         this.storeBee(BeehiveBlockEntity.Occupant.of(p_454988_));
         if (this.level != null) {
            if (p_454988_.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
               this.savedFlowerPos = p_454988_.getSavedFlowerPos();
            }

            BlockPos blockpos = this.getBlockPos();
            this.level.playSound(null, blockpos.getX(), blockpos.getY(), blockpos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_454988_, this.getBlockState()));
         }

         p_454988_.discard();
         super.setChanged();
      }
   }

   public void storeBee(BeehiveBlockEntity.Occupant p_329282_) {
      this.stored.add(new BeehiveBlockEntity.BeeData(p_329282_));
   }

   private static boolean releaseOccupant(
      Level p_155137_,
      BlockPos p_155138_,
      BlockState p_155139_,
      BeehiveBlockEntity.Occupant p_335681_,
      @Nullable List<Entity> p_155141_,
      BeehiveBlockEntity.BeeReleaseStatus p_155142_,
      @Nullable BlockPos p_155143_
   ) {
      if (p_155137_.environmentAttributes().getValue(EnvironmentAttributes.BEES_STAY_IN_HIVE, p_155138_)
         && p_155142_ != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      }

      Direction direction = p_155139_.getValue(BeehiveBlock.FACING);
      BlockPos blockpos = p_155138_.relative(direction);
      boolean flag = !p_155137_.getBlockState(blockpos).getCollisionShape(p_155137_, blockpos).isEmpty();
      if (flag && p_155142_ != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      }

      Entity entity = p_335681_.createEntity(p_155137_, p_155138_);
      if (entity != null) {
         if (entity instanceof Bee bee) {
            if (p_155143_ != null && !bee.hasSavedFlowerPos() && p_155137_.random.nextFloat() < 0.9F) {
               bee.setSavedFlowerPos(p_155143_);
            }

            if (p_155142_ == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
               bee.dropOffNectar();
               if (p_155139_.is(BlockTags.BEEHIVES, p_202037_ -> p_202037_.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                  int i = getHoneyLevel(p_155139_);
                  if (i < 5) {
                     int j = p_155137_.random.nextInt(100) == 0 ? 2 : 1;
                     if (i + j > 5) {
                        j--;
                     }

                     p_155137_.setBlockAndUpdate(p_155138_, p_155139_.setValue(BeehiveBlock.HONEY_LEVEL, i + j));
                  }
               }
            }

            if (p_155141_ != null) {
               p_155141_.add(bee);
            }

            float f = entity.getBbWidth();
            double d3 = flag ? 0.0 : 0.55 + f / 2.0F;
            double d0 = p_155138_.getX() + 0.5 + d3 * direction.getStepX();
            double d1 = p_155138_.getY() + 0.5 - entity.getBbHeight() / 2.0F;
            double d2 = p_155138_.getZ() + 0.5 + d3 * direction.getStepZ();
            entity.snapTo(d0, d1, d2, entity.getYRot(), entity.getXRot());
         }

         p_155137_.playSound(null, p_155138_, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_155137_.gameEvent(GameEvent.BLOCK_CHANGE, p_155138_, GameEvent.Context.of(entity, p_155137_.getBlockState(p_155138_)));
         return p_155137_.addFreshEntity(entity);
      } else {
         return false;
      }
   }

   private boolean hasSavedFlowerPos() {
      return this.savedFlowerPos != null;
   }

   private static void tickOccupants(
      Level p_155150_, BlockPos p_155151_, BlockState p_155152_, List<BeehiveBlockEntity.BeeData> p_155153_, @Nullable BlockPos p_155154_
   ) {
      boolean flag = false;
      Iterator<BeehiveBlockEntity.BeeData> iterator = p_155153_.iterator();

      while (iterator.hasNext()) {
         BeehiveBlockEntity.BeeData beehiveblockentity$beedata = iterator.next();
         if (beehiveblockentity$beedata.tick()) {
            BeehiveBlockEntity.BeeReleaseStatus beehiveblockentity$beereleasestatus = beehiveblockentity$beedata.hasNectar()
               ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED
               : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
            if (releaseOccupant(p_155150_, p_155151_, p_155152_, beehiveblockentity$beedata.toOccupant(), null, beehiveblockentity$beereleasestatus, p_155154_)
               )
             {
               flag = true;
               iterator.remove();
            }
         }
      }

      if (flag) {
         setChanged(p_155150_, p_155151_, p_155152_);
      }
   }

   public static void serverTick(Level p_155145_, BlockPos p_155146_, BlockState p_155147_, BeehiveBlockEntity p_155148_) {
      tickOccupants(p_155145_, p_155146_, p_155147_, p_155148_.stored, p_155148_.savedFlowerPos);
      if (!p_155148_.stored.isEmpty() && p_155145_.getRandom().nextDouble() < 0.005) {
         double d0 = p_155146_.getX() + 0.5;
         double d1 = p_155146_.getY();
         double d2 = p_155146_.getZ() + 0.5;
         p_155145_.playSound(null, d0, d1, d2, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   protected void loadAdditional(ValueInput p_406058_) {
      super.loadAdditional(p_406058_);
      this.stored.clear();
      p_406058_.<List>read("bees", BeehiveBlockEntity.Occupant.LIST_CODEC).orElse(List.of()).forEach(this::storeBee);
      this.savedFlowerPos = p_406058_.<BlockPos>read("flower_pos", BlockPos.CODEC).orElse(null);
   }

   @Override
   protected void saveAdditional(ValueOutput p_410673_) {
      super.saveAdditional(p_410673_);
      p_410673_.store("bees", BeehiveBlockEntity.Occupant.LIST_CODEC, this.getBees());
      p_410673_.storeNullable("flower_pos", BlockPos.CODEC, this.savedFlowerPos);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_395385_) {
      super.applyImplicitComponents(p_395385_);
      this.stored.clear();
      List<BeehiveBlockEntity.Occupant> list = p_395385_.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
      list.forEach(this::storeBee);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_328977_) {
      super.collectImplicitComponents(p_328977_);
      p_328977_.set(DataComponents.BEES, new Bees(this.getBees()));
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_407452_) {
      super.removeComponentsFromTag(p_407452_);
      p_407452_.discard("bees");
   }

   private List<BeehiveBlockEntity.Occupant> getBees() {
      return this.stored.stream().map(BeehiveBlockEntity.BeeData::toOccupant).toList();
   }

   @Override
   public void registerDebugValues(ServerLevel p_425406_, DebugValueSource.Registration p_423700_) {
      p_423700_.register(DebugSubscriptions.BEE_HIVES, () -> DebugHiveInfo.pack(this));
   }

   static class BeeData {
      private final BeehiveBlockEntity.Occupant occupant;
      private int ticksInHive;

      BeeData(BeehiveBlockEntity.Occupant p_336059_) {
         this.occupant = p_336059_;
         this.ticksInHive = p_336059_.ticksInHive();
      }

      public boolean tick() {
         return this.ticksInHive++ > this.occupant.minTicksInHive;
      }

      public BeehiveBlockEntity.Occupant toOccupant() {
         return new BeehiveBlockEntity.Occupant(this.occupant.entityData, this.ticksInHive, this.occupant.minTicksInHive);
      }

      public boolean hasNectar() {
         return this.occupant.entityData.getUnsafe().getBooleanOr("HasNectar", false);
      }
   }

   public enum BeeReleaseStatus {
      HONEY_DELIVERED,
      BEE_RELEASED,
      EMERGENCY;
   }

   public record Occupant(TypedEntityData<EntityType<?>> entityData, int ticksInHive, int minTicksInHive) {
      public static final Codec<BeehiveBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
         p_422150_ -> p_422150_.group(
               TypedEntityData.codec(EntityType.CODEC).fieldOf("entity_data").forGetter(BeehiveBlockEntity.Occupant::entityData),
               Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::ticksInHive),
               Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::minTicksInHive)
            )
            .apply(p_422150_, BeehiveBlockEntity.Occupant::new)
      );
      public static final Codec<List<BeehiveBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
      public static final StreamCodec<RegistryFriendlyByteBuf, BeehiveBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
         TypedEntityData.streamCodec(EntityType.STREAM_CODEC),
         BeehiveBlockEntity.Occupant::entityData,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::ticksInHive,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::minTicksInHive,
         BeehiveBlockEntity.Occupant::new
      );

      public static BeehiveBlockEntity.Occupant of(Entity p_331052_) {
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
               p_331052_.problemPath(), BeehiveBlockEntity.LOGGER
            )) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_331052_.registryAccess());
            p_331052_.save(tagvalueoutput);
            BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach(tagvalueoutput::discard);
            CompoundTag compoundtag = tagvalueoutput.buildResult();
            boolean flag = compoundtag.getBooleanOr("HasNectar", false);
            return new BeehiveBlockEntity.Occupant(TypedEntityData.of(p_331052_.getType(), compoundtag), 0, flag ? 2400 : 600);
         }
      }

      public static BeehiveBlockEntity.Occupant create(int p_330047_) {
         return new BeehiveBlockEntity.Occupant(TypedEntityData.of(EntityType.BEE, new CompoundTag()), p_330047_, 600);
      }

      public @Nullable Entity createEntity(Level p_328931_, BlockPos p_336164_) {
         CompoundTag compoundtag = this.entityData.copyTagWithoutId();
         BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach(compoundtag::remove);
         Entity entity = EntityType.loadEntityRecursive(this.entityData.type(), compoundtag, p_328931_, EntitySpawnReason.LOAD, EntityProcessor.NOP);
         if (entity != null && entity.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
            entity.setNoGravity(true);
            if (entity instanceof Bee bee) {
               bee.setHivePos(p_336164_);
               setBeeReleaseData(this.ticksInHive, bee);
            }

            return entity;
         } else {
            return null;
         }
      }

      private static void setBeeReleaseData(int p_330253_, Bee p_451587_) {
         int i = p_451587_.getAge();
         if (i < 0) {
            p_451587_.setAge(Math.min(0, i + p_330253_));
         } else if (i > 0) {
            p_451587_.setAge(Math.max(0, i - p_330253_));
         }

         p_451587_.setInLoveTime(Math.max(0, p_451587_.getInLoveTime() - p_330253_));
      }
   }
}
