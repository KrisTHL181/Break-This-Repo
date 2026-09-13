package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class Stitcher<T extends Stitcher.Entry> {
   private static final Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR = Comparator.<Stitcher.Holder<?>, Integer>comparing(h -> -h.height)
      .thenComparing(h -> -h.width)
      .thenComparing(h -> h.entry.name());
   private final int mipLevel;
   private final List<Stitcher.Holder<T>> texturesToBeStitched = new ArrayList<>();
   private final List<Stitcher.Region<T>> storage = new ArrayList<>();
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;
   private final int padding;

   public Stitcher(final int maxWidth, final int maxHeight, final int mipLevel, final int anisotropyBit) {
      this.mipLevel = mipLevel;
      this.maxWidth = maxWidth;
      this.maxHeight = maxHeight;
      this.padding = 1 << mipLevel << Mth.clamp(anisotropyBit - 1, 0, 4);
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(final T entry) {
      Stitcher.Holder<T> holder = new Stitcher.Holder<>(
         entry,
         smallestFittingMinTexel(entry.width() + this.padding * 2, this.mipLevel),
         smallestFittingMinTexel(entry.height() + this.padding * 2, this.mipLevel)
      );
      this.texturesToBeStitched.add(holder);
   }

   public void stitch() {
      List<Stitcher.Holder<T>> holders = new ArrayList<>(this.texturesToBeStitched);
      holders.sort(HOLDER_COMPARATOR);

      for (Stitcher.Holder<T> holder : holders) {
         if (!this.addToStorage(holder)) {
            throw new StitcherException(holder.entry, holders.stream().map(h -> h.entry).collect(ImmutableList.toImmutableList()));
         }
      }
   }

   public void gatherSprites(final Stitcher.SpriteLoader<T> loader) {
      for (Stitcher.Region<T> topRegion : this.storage) {
         topRegion.walk(loader, this.padding);
      }
   }

   private static int smallestFittingMinTexel(final int input, final int maxMipLevel) {
      return (input >> maxMipLevel) + ((input & (1 << maxMipLevel) - 1) == 0 ? 0 : 1) << maxMipLevel;
   }

   private boolean addToStorage(final Stitcher.Holder<T> holder) {
      for (Stitcher.Region<T> region : this.storage) {
         if (region.add(holder)) {
            return true;
         }
      }

      return this.expand(holder);
   }

   private boolean expand(final Stitcher.Holder<T> holder) {
      int xCurrentSize = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int yCurrentSize = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int xNewSize = Mth.smallestEncompassingPowerOfTwo(this.storageX + holder.width);
      int yNewSize = Mth.smallestEncompassingPowerOfTwo(this.storageY + holder.height);
      boolean xCanGrow = xNewSize <= this.maxWidth;
      boolean yCanGrow = yNewSize <= this.maxHeight;
      if (!xCanGrow && !yCanGrow) {
         return false;
      }

      boolean xWillGrow = xCanGrow && xCurrentSize != xNewSize;
      boolean yWillGrow = yCanGrow && yCurrentSize != yNewSize;
      boolean growOnX;
      if (xWillGrow ^ yWillGrow) {
         growOnX = xWillGrow;
      } else {
         growOnX = xCanGrow && xCurrentSize <= yCurrentSize;
      }

      Stitcher.Region<T> slot;
      if (growOnX) {
         if (this.storageY == 0) {
            this.storageY = yNewSize;
         }

         slot = new Stitcher.Region<>(this.storageX, 0, xNewSize - this.storageX, this.storageY);
         this.storageX = xNewSize;
      } else {
         slot = new Stitcher.Region<>(0, this.storageY, this.storageX, yNewSize - this.storageY);
         this.storageY = yNewSize;
      }

      slot.add(holder);
      this.storage.add(slot);
      return true;
   }

   public interface Entry {
      int width();

      int height();

      Identifier name();
   }

   private record Holder<T extends Stitcher.Entry>(T entry, int width, int height) {
   }

   public static class Region<T extends Stitcher.Entry> {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      private @Nullable List<Stitcher.Region<T>> subSlots;
      private Stitcher.@Nullable Holder<T> holder;

      public Region(final int originX, final int originY, final int width, final int height) {
         this.originX = originX;
         this.originY = originY;
         this.width = width;
         this.height = height;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(final Stitcher.Holder<T> holder) {
         if (this.holder != null) {
            return false;
         }

         int textureWidth = holder.width;
         int textureHeight = holder.height;
         if (textureWidth <= this.width && textureHeight <= this.height) {
            if (textureWidth == this.width && textureHeight == this.height) {
               this.holder = holder;
               return true;
            }

            if (this.subSlots == null) {
               this.subSlots = new ArrayList<>(1);
               this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY, textureWidth, textureHeight));
               int spareWidth = this.width - textureWidth;
               int spareHeight = this.height - textureHeight;
               if (spareHeight > 0 && spareWidth > 0) {
                  int right = Math.max(this.height, spareWidth);
                  int bottom = Math.max(this.width, spareHeight);
                  if (right >= bottom) {
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + textureHeight, textureWidth, spareHeight));
                     this.subSlots.add(new Stitcher.Region<>(this.originX + textureWidth, this.originY, spareWidth, this.height));
                  } else {
                     this.subSlots.add(new Stitcher.Region<>(this.originX + textureWidth, this.originY, spareWidth, textureHeight));
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + textureHeight, this.width, spareHeight));
                  }
               } else if (spareWidth == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + textureHeight, textureWidth, spareHeight));
               } else if (spareHeight == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX + textureWidth, this.originY, spareWidth, textureHeight));
               }
            }

            for (Stitcher.Region<T> subSlot : this.subSlots) {
               if (subSlot.add(holder)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      public void walk(final Stitcher.SpriteLoader<T> output, final int padding) {
         if (this.holder != null) {
            output.load(this.holder.entry, this.getX(), this.getY(), padding);
         } else if (this.subSlots != null) {
            for (Stitcher.Region<T> subSlot : this.subSlots) {
               subSlot.walk(output, padding);
            }
         }
      }

      @Override
      public String toString() {
         return "Slot{originX="
            + this.originX
            + ", originY="
            + this.originY
            + ", width="
            + this.width
            + ", height="
            + this.height
            + ", texture="
            + this.holder
            + ", subSlots="
            + this.subSlots
            + "}";
      }
   }

   public interface SpriteLoader<T extends Stitcher.Entry> {
      void load(T entry, int x, int z, int padding);
   }
}
