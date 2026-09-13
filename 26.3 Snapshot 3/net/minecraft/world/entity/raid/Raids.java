package net.minecraft.world.entity.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Raids extends SavedData {
   private static final Identifier RAID_FILE_ID = Identifier.withDefaultNamespace("raids");
   public static final Codec<Raids> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Raids.RaidWithId.CODEC
               .listOf()
               .optionalFieldOf("raids", List.of())
               .forGetter(r -> r.raidMap.int2ObjectEntrySet().stream().map(Raids.RaidWithId::from).toList()),
            Codec.INT.fieldOf("next_id").forGetter(r -> r.nextId),
            Codec.INT.fieldOf("tick").forGetter(r -> r.tick)
         )
         .apply(i, Raids::new)
   );
   public static final SavedDataType<Raids> TYPE = new SavedDataType<>(RAID_FILE_ID, Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
   private final Int2ObjectMap<Raid> raidMap = new Int2ObjectOpenHashMap();
   private int nextId = 1;
   private int tick;

   public Raids() {
      this.setDirty();
   }

   private Raids(final List<Raids.RaidWithId> raids, final int nextId, final int tick) {
      for (Raids.RaidWithId raid : raids) {
         this.raidMap.put(raid.id, raid.raid);
      }

      this.nextId = nextId;
      this.tick = tick;
   }

   public @Nullable Raid get(final int raidId) {
      return (Raid)this.raidMap.get(raidId);
   }

   public OptionalInt getId(final Raid raid) {
      ObjectIterator var2 = this.raidMap.int2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<Raid> entry = (Entry<Raid>)var2.next();
         if (entry.getValue() == raid) {
            return OptionalInt.of(entry.getIntKey());
         }
      }

      return OptionalInt.empty();
   }

   public void tick(final ServerLevel level) {
      this.tick++;
      Iterator<Raid> raidIterator = this.raidMap.values().iterator();

      while (raidIterator.hasNext()) {
         Raid raid = raidIterator.next();
         if (!level.getGameRules().get(GameRules.RAIDS)) {
            raid.stop();
         }

         if (raid.isStopped()) {
            raidIterator.remove();
            this.setDirty();
         } else {
            raid.tick(level);
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }
   }

   public static boolean canJoinRaid(final Raider raider) {
      return raider.isAlive() && raider.canJoinRaid() && raider.getNoActionTime() <= 2400;
   }

   public @Nullable Raid createOrExtendRaid(final ServerPlayer player, final BlockPos raidPosition) {
      if (player.isSpectator()) {
         return null;
      }

      ServerLevel level = player.level();
      if (!level.getGameRules().get(GameRules.RAIDS)) {
         return null;
      }

      if (!level.environmentAttributes().getValue(EnvironmentAttributes.CAN_START_RAID, raidPosition)) {
         return null;
      }

      List<PoiRecord> posses = level.getPoiManager().getInRange(e -> e.is(PoiTypeTags.VILLAGE), raidPosition, 64, PoiManager.Occupancy.IS_OCCUPIED).toList();
      int count = 0;
      Vec3 posTotals = Vec3.ZERO;

      for (PoiRecord p : posses) {
         BlockPos pos = p.getPos();
         posTotals = posTotals.add(pos.getX(), pos.getY(), pos.getZ());
         count++;
      }

      BlockPos raidCenterPos;
      if (count > 0) {
         posTotals = posTotals.scale(1.0 / count);
         raidCenterPos = BlockPos.containing(posTotals);
      } else {
         raidCenterPos = raidPosition;
      }

      Raid raid = this.getOrCreateRaid(level, raidCenterPos);
      if (!raid.isStarted() && !this.raidMap.containsValue(raid)) {
         this.raidMap.put(this.getUniqueId(), raid);
      }

      if (!raid.isStarted() || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
         raid.absorbRaidOmen(player);
      }

      this.setDirty();
      return raid;
   }

   private Raid getOrCreateRaid(final ServerLevel level, final BlockPos pos) {
      Raid raid = level.getRaidAt(pos);
      return raid != null ? raid : new Raid(pos, level.getDifficulty());
   }

   public static Raids load(final CompoundTag tag) {
      return CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElseGet(Raids::new);
   }

   private int getUniqueId() {
      return ++this.nextId;
   }

   public @Nullable Raid getNearbyRaid(final BlockPos pos, final int maxDistSqr) {
      Raid closest = null;
      double closestDistanceSqr = maxDistSqr;
      ObjectIterator var6 = this.raidMap.values().iterator();

      while (var6.hasNext()) {
         Raid raid = (Raid)var6.next();
         double distance = raid.getCenter().distSqr(pos);
         if (raid.isActive() && distance < closestDistanceSqr) {
            closest = raid;
            closestDistanceSqr = distance;
         }
      }

      return closest;
   }

   @VisibleForDebug
   public List<BlockPos> getRaidCentersInChunk(final ChunkPos chunkPos) {
      return this.raidMap.values().stream().map(Raid::getCenter).filter(chunkPos::contains).toList();
   }

   private record RaidWithId(int id, Raid raid) {
      public static final Codec<Raids.RaidWithId> CODEC = RecordCodecBuilder.create(
         i -> i.group(Codec.INT.fieldOf("id").forGetter(Raids.RaidWithId::id), Raid.MAP_CODEC.forGetter(Raids.RaidWithId::raid))
            .apply(i, Raids.RaidWithId::new)
      );

      public static Raids.RaidWithId from(final Entry<Raid> entry) {
         return new Raids.RaidWithId(entry.getIntKey(), (Raid)entry.getValue());
      }
   }
}
