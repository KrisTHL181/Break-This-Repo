package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Stitcher<T extends Stitcher.Entry> {
   private static final Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR = Comparator.<Stitcher.Holder<?>, Integer>comparing(p_118201_ -> -p_118201_.height)
      .thenComparing(p_118199_ -> -p_118199_.width)
      .thenComparing(p_448389_ -> p_448389_.entry.name());
   private final int mipLevel;
   private final List<Stitcher.Holder<T>> texturesToBeStitched = new ArrayList<>();
   private final List<Stitcher.Region<T>> storage = new ArrayList<>();
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;
   private final int padding;

   public Stitcher(int p_118171_, int p_118172_, int p_118173_, int p_459323_) {
      this.mipLevel = p_118173_;
      this.maxWidth = p_118171_;
      this.maxHeight = p_118172_;
      this.padding = 1 << p_118173_ << Mth.clamp(p_459323_ - 1, 0, 4);
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(T p_249253_) {
      Stitcher.Holder<T> holder = new Stitcher.Holder<>(
         p_249253_,
         smallestFittingMinTexel(p_249253_.width() + this.padding * 2, this.mipLevel),
         smallestFittingMinTexel(p_249253_.height() + this.padding * 2, this.mipLevel)
      );
      this.texturesToBeStitched.add(holder);
   }

   public void stitch() {
      List<Stitcher.Holder<T>> list = new ArrayList<>(this.texturesToBeStitched);
      list.sort(HOLDER_COMPARATOR);

      for (Stitcher.Holder<T> holder : list) {
         if (!this.addToStorage(holder)) {
            throw new StitcherException(holder.entry, list.stream().map(p_247946_ -> p_247946_.entry).collect(ImmutableList.toImmutableList()));
         }
      }
   }

   public void gatherSprites(Stitcher.SpriteLoader<T> p_118181_) {
      for (Stitcher.Region<T> region : this.storage) {
         region.walk(p_118181_, this.padding);
      }
   }

   private static int smallestFittingMinTexel(int p_118189_, int p_118190_) {
      return (p_118189_ >> p_118190_) + ((p_118189_ & (1 << p_118190_) - 1) == 0 ? 0 : 1) << p_118190_;
   }

   private boolean addToStorage(Stitcher.Holder<T> p_118179_) {
      for (Stitcher.Region<T> region : this.storage) {
         if (region.add(p_118179_)) {
            return true;
         }
      }

      return this.expand(p_118179_);
   }

   private boolean expand(Stitcher.Holder<T> p_118192_) {
      int i = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int j = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int k = Mth.smallestEncompassingPowerOfTwo(this.storageX + p_118192_.width);
      int l = Mth.smallestEncompassingPowerOfTwo(this.storageY + p_118192_.height);
      boolean flag1 = k <= this.maxWidth;
      boolean flag2 = l <= this.maxHeight;
      if (!flag1 && !flag2) {
         return false;
      }

      boolean flag3 = flag1 && i != k;
      boolean flag4 = flag2 && j != l;
      boolean flag;
      if (flag3 ^ flag4) {
         flag = flag3;
      } else {
         flag = flag1 && i <= j;
      }

      Stitcher.Region<T> region;
      if (flag) {
         if (this.storageY == 0) {
            this.storageY = l;
         }

         region = new Stitcher.Region<>(this.storageX, 0, k - this.storageX, this.storageY);
         this.storageX = k;
      } else {
         region = new Stitcher.Region<>(0, this.storageY, this.storageX, l - this.storageY);
         this.storageY = l;
      }

      region.add(p_118192_);
      this.storage.add(region);
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public interface Entry {
      int width();

      int height();

      Identifier name();
   }

   @OnlyIn(Dist.CLIENT)
   record Holder<T extends Stitcher.Entry>(T entry, int width, int height) {
   }

   @OnlyIn(Dist.CLIENT)
   public static class Region<T extends Stitcher.Entry> {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      private @Nullable List<Stitcher.Region<T>> subSlots;
      private Stitcher.@Nullable Holder<T> holder;

      public Region(int p_118216_, int p_118217_, int p_118218_, int p_118219_) {
         this.originX = p_118216_;
         this.originY = p_118217_;
         this.width = p_118218_;
         this.height = p_118219_;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(Stitcher.Holder<T> p_118222_) {
         if (this.holder != null) {
            return false;
         }

         int i = p_118222_.width;
         int j = p_118222_.height;
         if (i <= this.width && j <= this.height) {
            if (i == this.width && j == this.height) {
               this.holder = p_118222_;
               return true;
            }

            if (this.subSlots == null) {
               this.subSlots = new ArrayList<>(1);
               this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY, i, j));
               int k = this.width - i;
               int l = this.height - j;
               if (l > 0 && k > 0) {
                  int i1 = Math.max(this.height, k);
                  int j1 = Math.max(this.width, l);
                  if (i1 >= j1) {
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + j, i, l));
                     this.subSlots.add(new Stitcher.Region<>(this.originX + i, this.originY, k, this.height));
                  } else {
                     this.subSlots.add(new Stitcher.Region<>(this.originX + i, this.originY, k, j));
                     this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + j, this.width, l));
                  }
               } else if (k == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX, this.originY + j, i, l));
               } else if (l == 0) {
                  this.subSlots.add(new Stitcher.Region<>(this.originX + i, this.originY, k, j));
               }
            }

            for (Stitcher.Region<T> region : this.subSlots) {
               if (region.add(p_118222_)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      public void walk(Stitcher.SpriteLoader<T> p_250195_, int p_452707_) {
         if (this.holder != null) {
            p_250195_.load(this.holder.entry, this.getX(), this.getY(), p_452707_);
         } else if (this.subSlots != null) {
            for (Stitcher.Region<T> region : this.subSlots) {
               region.walk(p_250195_, p_452707_);
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

   @OnlyIn(Dist.CLIENT)
   public interface SpriteLoader<T extends Stitcher.Entry> {
      void load(T var1, int var2, int var3, int var4);
   }
}
