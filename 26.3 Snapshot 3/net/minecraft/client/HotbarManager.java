package net.minecraft.client;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.nio.file.Path;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import org.slf4j.Logger;

public class HotbarManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NUM_HOTBAR_GROUPS = 9;
   private final Path optionsFile;
   private final DataFixer fixerUpper;
   private final Hotbar[] hotbars = new Hotbar[9];
   private boolean loaded;

   public HotbarManager(final Path workingDirectory, final DataFixer fixerUpper) {
      this.optionsFile = workingDirectory.resolve("hotbar.nbt");
      this.fixerUpper = fixerUpper;

      for (int i = 0; i < 9; i++) {
         this.hotbars[i] = new Hotbar();
      }
   }

   private void load() {
      try {
         CompoundTag tag = NbtIo.read(this.optionsFile);
         if (tag == null) {
            return;
         }

         int version = NbtUtils.getDataVersion(tag, 1343);
         tag = DataFixTypes.HOTBAR.updateToCurrentVersion(this.fixerUpper, tag, version);

         for (int i = 0; i < 9; i++) {
            this.hotbars[i] = Hotbar.CODEC
               .parse(NbtOps.INSTANCE, tag.get(String.valueOf(i)))
               .resultOrPartial(error -> LOGGER.warn("Failed to parse hotbar: {}", error))
               .orElseGet(Hotbar::new);
         }
      } catch (Exception e) {
         LOGGER.error("Failed to load creative mode options", e);
      }
   }

   public void save() {
      try {
         CompoundTag tag = NbtUtils.addCurrentDataVersion(new CompoundTag());

         for (int i = 0; i < 9; i++) {
            Hotbar hotbar = this.get(i);
            DataResult<Tag> result = Hotbar.CODEC.encodeStart(NbtOps.INSTANCE, hotbar);
            tag.put(String.valueOf(i), (Tag)result.getOrThrow());
         }

         NbtIo.write(tag, this.optionsFile);
      } catch (Exception e) {
         LOGGER.error("Failed to save creative mode options", e);
      }
   }

   public Hotbar get(final int id) {
      if (!this.loaded) {
         this.load();
         this.loaded = true;
      }

      return this.hotbars[id];
   }
}
