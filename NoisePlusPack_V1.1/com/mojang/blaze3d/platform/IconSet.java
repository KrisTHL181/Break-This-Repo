package com.mojang.blaze3d.platform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public enum IconSet {
   RELEASE("icons"),
   SNAPSHOT("icons", "snapshot");

   private final String[] path;

   IconSet(final String... p_281663_) {
      this.path = p_281663_;
   }

   public List<IoSupplier<InputStream>> getStandardIcons(PackResources p_281372_) throws IOException {
      return List.of(
         this.getFile(p_281372_, "icon_16x16.png"),
         this.getFile(p_281372_, "icon_32x32.png"),
         this.getFile(p_281372_, "icon_48x48.png"),
         this.getFile(p_281372_, "icon_128x128.png"),
         this.getFile(p_281372_, "icon_256x256.png")
      );
   }

   public IoSupplier<InputStream> getMacIcon(PackResources p_281289_) throws IOException {
      return this.getFile(p_281289_, "minecraft.icns");
   }

   private IoSupplier<InputStream> getFile(PackResources p_281570_, String p_281345_) throws IOException {
      String[] astring = (String[])ArrayUtils.add(this.path, p_281345_);
      IoSupplier<InputStream> iosupplier = p_281570_.getRootResource(astring);
      if (iosupplier == null) {
         throw new FileNotFoundException(String.join("/", astring));
      } else {
         return iosupplier;
      }
   }
}
