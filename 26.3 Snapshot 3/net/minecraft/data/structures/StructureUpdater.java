package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = PackType.SERVER_DATA.getDirectory() + "/minecraft/structure/";

   @Override
   public CompoundTag apply(final String name, final CompoundTag input) {
      return name.startsWith(PREFIX) ? update(name, input) : input;
   }

   public static CompoundTag update(final String name, final CompoundTag tag) {
      StructureTemplate structureTemplate = new StructureTemplate();
      int fromVersion = NbtUtils.getDataVersion(tag, 500);
      int toVersion = 4997;
      if (fromVersion < 4997) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", new Object[]{fromVersion, 4997, name});
      }

      CompoundTag updated = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, fromVersion);
      structureTemplate.load(BuiltInRegistries.BLOCK, updated);
      return structureTemplate.save(new CompoundTag());
   }
}
