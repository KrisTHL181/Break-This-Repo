package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapTextureManager implements AutoCloseable {
   private final Int2ObjectMap<MapTextureManager.MapInstance> maps = new Int2ObjectOpenHashMap();
   final TextureManager textureManager;

   public MapTextureManager(TextureManager p_362721_) {
      this.textureManager = p_362721_;
   }

   public void update(MapId p_368009_, MapItemSavedData p_365364_) {
      this.getOrCreateMapInstance(p_368009_, p_365364_).forceUpload();
   }

   public Identifier prepareMapTexture(MapId p_361877_, MapItemSavedData p_361499_) {
      MapTextureManager.MapInstance maptexturemanager$mapinstance = this.getOrCreateMapInstance(p_361877_, p_361499_);
      maptexturemanager$mapinstance.updateTextureIfNeeded();
      return maptexturemanager$mapinstance.location;
   }

   public void resetData() {
      ObjectIterator var1 = this.maps.values().iterator();

      while (var1.hasNext()) {
         MapTextureManager.MapInstance maptexturemanager$mapinstance = (MapTextureManager.MapInstance)var1.next();
         maptexturemanager$mapinstance.close();
      }

      this.maps.clear();
   }

   private MapTextureManager.MapInstance getOrCreateMapInstance(MapId p_370051_, MapItemSavedData p_367972_) {
      return (MapTextureManager.MapInstance)this.maps.compute(p_370051_.id(), (p_366926_, p_369937_) -> {
         if (p_369937_ == null) {
            return new MapTextureManager.MapInstance(p_366926_, p_367972_);
         }

         p_369937_.replaceMapData(p_367972_);
         return p_369937_;
      });
   }

   @Override
   public void close() {
      this.resetData();
   }

   @OnlyIn(Dist.CLIENT)
   class MapInstance implements AutoCloseable {
      private MapItemSavedData data;
      private final DynamicTexture texture;
      private boolean requiresUpload = true;
      final Identifier location;

      MapInstance(final int p_361202_, final MapItemSavedData p_361755_) {
         this.data = p_361755_;
         this.texture = new DynamicTexture(() -> "Map " + p_361202_, 128, 128, true);
         this.location = Identifier.withDefaultNamespace("map/" + p_361202_);
         MapTextureManager.this.textureManager.register(this.location, this.texture);
      }

      void replaceMapData(MapItemSavedData p_369715_) {
         boolean flag = this.data != p_369715_;
         this.data = p_369715_;
         this.requiresUpload |= flag;
      }

      public void forceUpload() {
         this.requiresUpload = true;
      }

      void updateTextureIfNeeded() {
         if (this.requiresUpload) {
            NativeImage nativeimage = this.texture.getPixels();
            if (nativeimage != null) {
               for (int i = 0; i < 128; i++) {
                  for (int j = 0; j < 128; j++) {
                     int k = j + i * 128;
                     nativeimage.setPixel(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
                  }
               }
            }

            this.texture.upload();
            this.requiresUpload = false;
         }
      }

      @Override
      public void close() {
         this.texture.close();
      }
   }
}
