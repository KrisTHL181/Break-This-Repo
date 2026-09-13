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
   public CompoundTag apply(String p_126503_, CompoundTag p_126504_) {
      return p_126503_.startsWith(PREFIX) ? update(p_126503_, p_126504_) : p_126504_;
   }

   public static CompoundTag update(String p_176823_, CompoundTag p_176824_) {
      StructureTemplate structuretemplate = new StructureTemplate();
      int i = NbtUtils.getDataVersion(p_176824_, 500);
      int j = 4650;
      if (i < 4650) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", new Object[]{i, 4650, p_176823_});
      }

      CompoundTag compoundtag = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), p_176824_, i);
      structuretemplate.load(BuiltInRegistries.BLOCK, compoundtag);
      return structuretemplate.save(new CompoundTag());
   }
}
