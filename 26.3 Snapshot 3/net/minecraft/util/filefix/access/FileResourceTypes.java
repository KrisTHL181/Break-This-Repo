package net.minecraft.util.filefix.access;

import com.mojang.datafixers.DSL.TypeReference;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

public class FileResourceTypes {
   public static final FileResourceType<LevelDat> LEVEL_DAT = new FileResourceType<>(LevelDat::new);
   public static final FileResourceType<PlayerData> PLAYER_DATA = new FileResourceType<>(PlayerData::new);

   public static FileResourceType<SavedDataNbt> savedData(final TypeReference type) {
      return savedData(type, CompressedNbt.MissingSeverity.NEUTRAL);
   }

   public static FileResourceType<SavedDataNbt> savedData(final TypeReference type, final CompressedNbt.MissingSeverity missingSeverity) {
      return new FileResourceType<>((path, dataVersion) -> new SavedDataNbt(type, path, dataVersion, missingSeverity));
   }

   public static FileResourceType<ChunkNbt> chunk(final DataFixTypes type, final RegionStorageInfo info) {
      return new FileResourceType<>((path, dataVersion) -> new ChunkNbt(info, path, type, dataVersion));
   }
}
