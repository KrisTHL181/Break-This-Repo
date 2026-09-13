package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.jspecify.annotations.Nullable;

public record DebugBrainDump(
   String name,
   String profession,
   int xp,
   float health,
   float maxHealth,
   String inventory,
   boolean wantsGolem,
   int angerLevel,
   List<String> activities,
   List<String> behaviors,
   List<String> memories,
   List<String> gossips,
   Set<BlockPos> pois,
   Set<BlockPos> potentialPois
) {
   public static final StreamCodec<FriendlyByteBuf, DebugBrainDump> STREAM_CODEC = StreamCodec.of(
      (p_426434_, p_423835_) -> p_423835_.write(p_426434_), DebugBrainDump::new
   );

   public DebugBrainDump(FriendlyByteBuf p_431101_) {
      this(
         p_431101_.readUtf(),
         p_431101_.readUtf(),
         p_431101_.readInt(),
         p_431101_.readFloat(),
         p_431101_.readFloat(),
         p_431101_.readUtf(),
         p_431101_.readBoolean(),
         p_431101_.readInt(),
         p_431101_.readList(FriendlyByteBuf::readUtf),
         p_431101_.readList(FriendlyByteBuf::readUtf),
         p_431101_.readList(FriendlyByteBuf::readUtf),
         p_431101_.readList(FriendlyByteBuf::readUtf),
         p_431101_.readCollection(HashSet::new, BlockPos.STREAM_CODEC),
         p_431101_.readCollection(HashSet::new, BlockPos.STREAM_CODEC)
      );
   }

   public void write(FriendlyByteBuf p_428816_) {
      p_428816_.writeUtf(this.name);
      p_428816_.writeUtf(this.profession);
      p_428816_.writeInt(this.xp);
      p_428816_.writeFloat(this.health);
      p_428816_.writeFloat(this.maxHealth);
      p_428816_.writeUtf(this.inventory);
      p_428816_.writeBoolean(this.wantsGolem);
      p_428816_.writeInt(this.angerLevel);
      p_428816_.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
      p_428816_.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
      p_428816_.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
      p_428816_.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
      p_428816_.writeCollection(this.pois, BlockPos.STREAM_CODEC);
      p_428816_.writeCollection(this.potentialPois, BlockPos.STREAM_CODEC);
   }

   public static DebugBrainDump takeBrainDump(ServerLevel p_431521_, LivingEntity p_425047_) {
      String s = DebugEntityNameGenerator.getEntityName(p_425047_);
      String s1;
      int i;
      if (p_425047_ instanceof Villager villager) {
         s1 = villager.getVillagerData().profession().getRegisteredName();
         i = villager.getVillagerXp();
      } else {
         s1 = "";
         i = 0;
      }

      float f1 = p_425047_.getHealth();
      float f = p_425047_.getMaxHealth();
      Brain<?> brain = p_425047_.getBrain();
      long j = p_425047_.level().getGameTime();
      String s2;
      if (p_425047_ instanceof InventoryCarrier inventorycarrier) {
         Container container = inventorycarrier.getInventory();
         s2 = container.isEmpty() ? "" : container.toString();
      } else {
         s2 = "";
      }

      boolean flag = p_425047_ instanceof Villager villager2 && villager2.wantsToSpawnGolem(j);
      int k = p_425047_ instanceof Warden warden ? warden.getClientAngerLevel() : -1;
      List<String> list3 = brain.getActiveActivities().stream().map(Activity::getName).toList();
      List<String> list = brain.getRunningBehaviors().stream().map(BehaviorControl::debugString).toList();
      List<String> list1 = getMemoryDescriptions(p_431521_, p_425047_, j).map(p_428302_ -> StringUtil.truncateStringIfNecessary(p_428302_, 255, true)).toList();
      Set<BlockPos> set = getKnownBlockPositions(brain, MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
      Set<BlockPos> set1 = getKnownBlockPositions(brain, MemoryModuleType.POTENTIAL_JOB_SITE);
      List<String> list2 = p_425047_ instanceof Villager villager1 ? getVillagerGossips(villager1) : List.of();
      return new DebugBrainDump(s, s1, i, f1, f, s2, flag, k, list3, list, list1, list2, set, set1);
   }

   @SafeVarargs
   private static Set<BlockPos> getKnownBlockPositions(Brain<?> p_431099_, MemoryModuleType<GlobalPos>... p_424695_) {
      return Stream.of(p_424695_)
         .filter(p_431099_::hasMemoryValue)
         .map(p_431099_::getMemory)
         .flatMap(Optional::stream)
         .map(GlobalPos::pos)
         .collect(Collectors.toSet());
   }

   private static List<String> getVillagerGossips(Villager p_459952_) {
      List<String> list = new ArrayList<>();
      p_459952_.getGossips().getGossipEntries().forEach((p_449337_, p_449338_) -> {
         String s = DebugEntityNameGenerator.getEntityName(p_449337_);
         p_449338_.forEach((p_449341_, p_449342_) -> list.add(s + ": " + p_449341_ + ": " + p_449342_));
      });
      return list;
   }

   private static Stream<String> getMemoryDescriptions(ServerLevel p_424706_, LivingEntity p_430355_, long p_423408_) {
      return p_430355_.getBrain().getMemories().entrySet().stream().map(p_423474_ -> {
         MemoryModuleType<?> memorymoduletype = p_423474_.getKey();
         Optional<? extends ExpirableValue<?>> optional = p_423474_.getValue();
         return getMemoryDescription(p_424706_, p_423408_, memorymoduletype, optional);
      }).sorted();
   }

   private static String getMemoryDescription(
      ServerLevel p_426266_, long p_426945_, MemoryModuleType<?> p_422812_, Optional<? extends ExpirableValue<?>> p_430749_
   ) {
      String s;
      if (p_430749_.isPresent()) {
         ExpirableValue<?> expirablevalue = (ExpirableValue<?>)p_430749_.get();
         Object object = expirablevalue.getValue();
         if (p_422812_ == MemoryModuleType.HEARD_BELL_TIME) {
            long i = p_426945_ - (Long)object;
            s = i + " ticks ago";
         } else if (expirablevalue.canExpire()) {
            s = getShortDescription(p_426266_, object) + " (ttl: " + expirablevalue.getTimeToLive() + ")";
         } else {
            s = getShortDescription(p_426266_, object);
         }
      } else {
         s = "-";
      }

      return BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(p_422812_).getPath() + ": " + s;
   }

   private static String getShortDescription(ServerLevel p_431174_, @Nullable Object p_427720_) {
      return switch (p_427720_) {
         case null -> "-";
         case UUID uuid -> getShortDescription(p_431174_, p_431174_.getEntity(uuid));
         case Entity entity -> DebugEntityNameGenerator.getEntityName(entity);
         case WalkTarget walktarget -> getShortDescription(p_431174_, walktarget.getTarget());
         case EntityTracker entitytracker -> getShortDescription(p_431174_, entitytracker.getEntity());
         case GlobalPos globalpos -> getShortDescription(p_431174_, globalpos.pos());
         case BlockPosTracker blockpostracker -> getShortDescription(p_431174_, blockpostracker.currentBlockPosition());
         case DamageSource damagesource -> {
            Entity entity1 = damagesource.getEntity();
            yield entity1 == null ? p_427720_.toString() : getShortDescription(p_431174_, entity1);
         }
         case Collection<?> collection -> "["
            + (String)collection.stream().map(p_425911_ -> getShortDescription(p_431174_, p_425911_)).collect(Collectors.joining(", "))
            + "]";
         default -> p_427720_.toString();
      };
   }

   public boolean hasPoi(BlockPos p_430894_) {
      return this.pois.contains(p_430894_);
   }

   public boolean hasPotentialPoi(BlockPos p_430406_) {
      return this.potentialPois.contains(p_430406_);
   }
}
