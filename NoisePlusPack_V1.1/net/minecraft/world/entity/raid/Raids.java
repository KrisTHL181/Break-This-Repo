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
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   public static final Codec<Raids> CODEC = RecordCodecBuilder.create(
      p_390766_ -> p_390766_.group(
            Raids.RaidWithId.CODEC
               .listOf()
               .optionalFieldOf("raids", List.of())
               .forGetter(p_390768_ -> p_390768_.raidMap.int2ObjectEntrySet().stream().map(Raids.RaidWithId::from).toList()),
            Codec.INT.fieldOf("next_id").forGetter(p_390767_ -> p_390767_.nextId),
            Codec.INT.fieldOf("tick").forGetter(p_390765_ -> p_390765_.tick)
         )
         .apply(p_390766_, Raids::new)
   );
   public static final SavedDataType<Raids> TYPE = new SavedDataType<>("raids", Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
   public static final SavedDataType<Raids> TYPE_END = new SavedDataType<>("raids_end", Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
   private final Int2ObjectMap<Raid> raidMap = new Int2ObjectOpenHashMap();
   private int nextId = 1;
   private int tick;

   public static SavedDataType<Raids> getType(Holder<DimensionType> p_394405_) {
      return p_394405_.is(BuiltinDimensionTypes.END) ? TYPE_END : TYPE;
   }

   public Raids() {
      this.setDirty();
   }

   private Raids(List<Raids.RaidWithId> p_396517_, int p_396475_, int p_396601_) {
      for (Raids.RaidWithId raids$raidwithid : p_396517_) {
         this.raidMap.put(raids$raidwithid.id, raids$raidwithid.raid);
      }

      this.nextId = p_396475_;
      this.tick = p_396601_;
   }

   public @Nullable Raid get(int p_37959_) {
      return (Raid)this.raidMap.get(p_37959_);
   }

   public OptionalInt getId(Raid p_396551_) {
      ObjectIterator var2 = this.raidMap.int2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<Raid> entry = (Entry<Raid>)var2.next();
         if (entry.getValue() == p_396551_) {
            return OptionalInt.of(entry.getIntKey());
         }
      }

      return OptionalInt.empty();
   }

   public void tick(ServerLevel p_392544_) {
      this.tick++;
      Iterator<Raid> iterator = this.raidMap.values().iterator();

      while (iterator.hasNext()) {
         Raid raid = iterator.next();
         if (!p_392544_.getGameRules().get(GameRules.RAIDS)) {
            raid.stop();
         }

         if (raid.isStopped()) {
            iterator.remove();
            this.setDirty();
         } else {
            raid.tick(p_392544_);
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }
   }

   public static boolean canJoinRaid(Raider p_37966_) {
      return p_37966_.isAlive() && p_37966_.canJoinRaid() && p_37966_.getNoActionTime() <= 2400;
   }

   public @Nullable Raid createOrExtendRaid(ServerPlayer p_37964_, BlockPos p_336355_) {
      if (p_37964_.isSpectator()) {
         return null;
      }

      ServerLevel serverlevel = p_37964_.level();
      if (!serverlevel.getGameRules().get(GameRules.RAIDS)) {
         return null;
      }

      if (!serverlevel.environmentAttributes().getValue(EnvironmentAttributes.CAN_START_RAID, p_336355_)) {
         return null;
      }

      List<PoiRecord> list = serverlevel.getPoiManager()
         .getInRange(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE), p_336355_, 64, PoiManager.Occupancy.IS_OCCUPIED)
         .toList();
      int i = 0;
      Vec3 vec3 = Vec3.ZERO;

      for (PoiRecord poirecord : list) {
         BlockPos blockpos = poirecord.getPos();
         vec3 = vec3.add(blockpos.getX(), blockpos.getY(), blockpos.getZ());
         i++;
      }

      BlockPos blockpos1;
      if (i > 0) {
         vec3 = vec3.scale(1.0 / i);
         blockpos1 = BlockPos.containing(vec3);
      } else {
         blockpos1 = p_336355_;
      }

      Raid raid = this.getOrCreateRaid(serverlevel, blockpos1);
      if (!raid.isStarted() && !this.raidMap.containsValue(raid)) {
         this.raidMap.put(this.getUniqueId(), raid);
      }

      if (!raid.isStarted() || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
         raid.absorbRaidOmen(p_37964_);
      }

      this.setDirty();
      return raid;
   }

   private Raid getOrCreateRaid(ServerLevel p_37961_, BlockPos p_37962_) {
      Raid raid = p_37961_.getRaidAt(p_37962_);
      return raid != null ? raid : new Raid(p_37962_, p_37961_.getDifficulty());
   }

   public static Raids load(CompoundTag p_150237_) {
      return CODEC.parse(NbtOps.INSTANCE, p_150237_).resultOrPartial().orElseGet(Raids::new);
   }

   private int getUniqueId() {
      return ++this.nextId;
   }

   public @Nullable Raid getNearbyRaid(BlockPos p_37971_, int p_37972_) {
      Raid raid = null;
      double d0 = p_37972_;
      ObjectIterator var6 = this.raidMap.values().iterator();

      while (var6.hasNext()) {
         Raid raid1 = (Raid)var6.next();
         double d1 = raid1.getCenter().distSqr(p_37971_);
         if (raid1.isActive() && d1 < d0) {
            raid = raid1;
            d0 = d1;
         }
      }

      return raid;
   }

   @VisibleForDebug
   public List<BlockPos> getRaidCentersInChunk(ChunkPos p_429622_) {
      return this.raidMap.values().stream().map(Raid::getCenter).filter(p_429622_::contains).toList();
   }

   record RaidWithId(int id, Raid raid) {
      public static final Codec<Raids.RaidWithId> CODEC = RecordCodecBuilder.create(
         p_394377_ -> p_394377_.group(Codec.INT.fieldOf("id").forGetter(Raids.RaidWithId::id), Raid.MAP_CODEC.forGetter(Raids.RaidWithId::raid))
            .apply(p_394377_, Raids.RaidWithId::new)
      );

      public static Raids.RaidWithId from(Entry<Raid> p_397700_) {
         return new Raids.RaidWithId(p_397700_.getIntKey(), (Raid)p_397700_.getValue());
      }
   }
}
