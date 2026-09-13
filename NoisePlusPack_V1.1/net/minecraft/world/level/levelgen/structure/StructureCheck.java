package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final StructureTemplateManager structureTemplateManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final RandomState randomState;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap();
   private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<>();

   public StructureCheck(
      ChunkScanAccess p_226712_,
      RegistryAccess p_226713_,
      StructureTemplateManager p_226714_,
      ResourceKey<Level> p_226715_,
      ChunkGenerator p_226716_,
      RandomState p_226717_,
      LevelHeightAccessor p_226718_,
      BiomeSource p_226719_,
      long p_226720_,
      DataFixer p_226721_
   ) {
      this.storageAccess = p_226712_;
      this.registryAccess = p_226713_;
      this.structureTemplateManager = p_226714_;
      this.dimension = p_226715_;
      this.chunkGenerator = p_226716_;
      this.randomState = p_226717_;
      this.heightAccessor = p_226718_;
      this.biomeSource = p_226719_;
      this.seed = p_226720_;
      this.fixerUpper = p_226721_;
   }

   public StructureCheckResult checkStart(ChunkPos p_226730_, Structure p_226731_, StructurePlacement p_327807_, boolean p_226732_) {
      long i = p_226730_.toLong();
      Object2IntMap<Structure> object2intmap = (Object2IntMap<Structure>)this.loadedChunks.get(i);
      if (object2intmap != null) {
         return this.checkStructureInfo(object2intmap, p_226731_, p_226732_);
      }

      StructureCheckResult structurecheckresult = this.tryLoadFromStorage(p_226730_, p_226731_, p_226732_, i);
      if (structurecheckresult != null) {
         return structurecheckresult;
      }

      if (!p_327807_.applyAdditionalChunkRestrictions(p_226730_.x, p_226730_.z, this.seed)) {
         return StructureCheckResult.START_NOT_PRESENT;
      }

      boolean flag = this.featureChecks
         .computeIfAbsent(p_226731_, p_226739_ -> new Long2BooleanOpenHashMap())
         .computeIfAbsent(i, p_226728_ -> this.canCreateStructure(p_226730_, p_226731_));
      return !flag ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
   }

   private boolean canCreateStructure(ChunkPos p_226756_, Structure p_226757_) {
      return p_226757_.findValidGenerationPoint(
            new Structure.GenerationContext(
               this.registryAccess,
               this.chunkGenerator,
               this.biomeSource,
               this.randomState,
               this.structureTemplateManager,
               this.seed,
               p_226756_,
               this.heightAccessor,
               p_226757_.biomes()::contains
            )
         )
         .isPresent();
   }

   private @Nullable StructureCheckResult tryLoadFromStorage(ChunkPos p_226734_, Structure p_226735_, boolean p_226736_, long p_226737_) {
      CollectFields collectfields = new CollectFields(
         new FieldSelector(IntTag.TYPE, "DataVersion"),
         new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"),
         new FieldSelector("structures", CompoundTag.TYPE, "starts")
      );

      try {
         this.storageAccess.scanChunk(p_226734_, collectfields).join();
      } catch (Exception exception1) {
         LOGGER.warn("Failed to read chunk {}", p_226734_, exception1);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      if (collectfields.getResult() instanceof CompoundTag compoundtag) {
         int i = NbtUtils.getDataVersion(compoundtag);
         if (i <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         }

         SimpleRegionStorage.injectDatafixingContext(
            compoundtag, ChunkMap.getChunkDataFixContextTag(this.dimension, this.chunkGenerator.getTypeNameForDataFixer())
         );

         CompoundTag compoundtag1;
         try {
            compoundtag1 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, compoundtag, i);
         } catch (Exception exception) {
            LOGGER.warn("Failed to partially datafix chunk {}", p_226734_, exception);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         }

         Object2IntMap<Structure> object2intmap = this.loadStructures(compoundtag1);
         if (object2intmap == null) {
            return null;
         }

         this.storeFullResults(p_226737_, object2intmap);
         return this.checkStructureInfo(object2intmap, p_226735_, p_226736_);
      } else {
         return null;
      }
   }

   private @Nullable Object2IntMap<Structure> loadStructures(CompoundTag p_197312_) {
      Optional<CompoundTag> optional = p_197312_.getCompound("structures").flatMap(p_391059_ -> p_391059_.getCompound("starts"));
      if (optional.isEmpty()) {
         return null;
      }

      CompoundTag compoundtag = optional.get();
      if (compoundtag.isEmpty()) {
         return Object2IntMaps.emptyMap();
      }

      Object2IntMap<Structure> object2intmap = new Object2IntOpenHashMap();
      Registry<Structure> registry = this.registryAccess.lookupOrThrow(Registries.STRUCTURE);
      compoundtag.forEach((p_450018_, p_450019_) -> {
         Identifier identifier = Identifier.tryParse(p_450018_);
         if (identifier != null) {
            Structure structure = registry.getValue(identifier);
            if (structure != null) {
               p_450019_.asCompound().ifPresent(p_391062_ -> {
                  String s = p_391062_.getStringOr("id", "");
                  if (!"INVALID".equals(s)) {
                     int i = p_391062_.getIntOr("references", 0);
                     object2intmap.put(structure, i);
                  }
               });
            }
         }
      });
      return object2intmap;
   }

   private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> p_197299_) {
      return p_197299_.isEmpty() ? Object2IntMaps.emptyMap() : p_197299_;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> p_226752_, Structure p_226753_, boolean p_226754_) {
      int i = p_226752_.getOrDefault(p_226753_, -1);
      return i != -1 && (!p_226754_ || i == 0) ? StructureCheckResult.START_PRESENT : StructureCheckResult.START_NOT_PRESENT;
   }

   public void onStructureLoad(ChunkPos p_197283_, Map<Structure, StructureStart> p_197284_) {
      long i = p_197283_.toLong();
      Object2IntMap<Structure> object2intmap = new Object2IntOpenHashMap();
      p_197284_.forEach((p_226749_, p_226750_) -> {
         if (p_226750_.isValid()) {
            object2intmap.put(p_226749_, p_226750_.getReferences());
         }
      });
      this.storeFullResults(i, object2intmap);
   }

   private void storeFullResults(long p_197264_, Object2IntMap<Structure> p_197265_) {
      this.loadedChunks.put(p_197264_, deduplicateEmptyMap(p_197265_));
      this.featureChecks.values().forEach(p_209956_ -> p_209956_.remove(p_197264_));
   }

   public void incrementReference(ChunkPos p_226723_, Structure p_226724_) {
      this.loadedChunks.compute(p_226723_.toLong(), (p_226745_, p_226746_) -> {
         if (p_226746_ == null || p_226746_.isEmpty()) {
            p_226746_ = new Object2IntOpenHashMap();
         }

         p_226746_.computeInt(p_226724_, (p_226741_, p_226742_) -> p_226742_ == null ? 1 : p_226742_ + 1);
         return p_226746_;
      });
   }
}
