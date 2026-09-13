package net.minecraft.client.gui.font;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FontSet implements AutoCloseable {
   private static final float LARGE_FORWARD_ADVANCE = 32.0F;
   private static final BakedGlyph INVISIBLE_MISSING_GLYPH = new BakedGlyph() {
      @Override
      public GlyphInfo info() {
         return SpecialGlyphs.MISSING;
      }

      @Override
      public TextRenderable.@Nullable Styled createGlyph(
         float p_428634_, float p_425554_, int p_422985_, int p_426346_, Style p_428823_, float p_431261_, float p_429807_
      ) {
         return null;
      }
   };
   final GlyphStitcher stitcher;
   final UnbakedGlyph.Stitcher wrappedStitcher = new UnbakedGlyph.Stitcher() {
      @Override
      public BakedGlyph stitch(GlyphInfo p_427636_, GlyphBitmap p_424640_) {
         return Objects.requireNonNullElse(FontSet.this.stitcher.stitch(p_427636_, p_424640_), FontSet.this.missingGlyph);
      }

      @Override
      public BakedGlyph getMissing() {
         return FontSet.this.missingGlyph;
      }
   };
   private List<GlyphProvider.Conditional> allProviders = List.of();
   private List<GlyphProvider> activeProviders = List.of();
   private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
   private final CodepointMap<FontSet.SelectedGlyphs> glyphCache = new CodepointMap<>(FontSet.SelectedGlyphs[]::new, FontSet.SelectedGlyphs[][]::new);
   private final IntFunction<FontSet.SelectedGlyphs> glyphGetter = this::computeGlyphInfo;
   BakedGlyph missingGlyph = INVISIBLE_MISSING_GLYPH;
   private final Supplier<BakedGlyph> missingGlyphGetter = () -> this.missingGlyph;
   private final FontSet.SelectedGlyphs missingSelectedGlyphs = new FontSet.SelectedGlyphs(this.missingGlyphGetter, this.missingGlyphGetter);
   private @Nullable EffectGlyph whiteGlyph;
   private final GlyphSource anyGlyphs = new FontSet.Source(false);
   private final GlyphSource nonFishyGlyphs = new FontSet.Source(true);

   public FontSet(GlyphStitcher p_428498_) {
      this.stitcher = p_428498_;
   }

   public void reload(List<GlyphProvider.Conditional> p_332248_, Set<FontOption> p_329677_) {
      this.allProviders = p_332248_;
      this.reload(p_329677_);
   }

   public void reload(Set<FontOption> p_331404_) {
      this.activeProviders = List.of();
      this.resetTextures();
      this.activeProviders = this.selectProviders(this.allProviders, p_331404_);
   }

   private void resetTextures() {
      this.stitcher.reset();
      this.glyphCache.clear();
      this.glyphsByWidth.clear();
      this.missingGlyph = Objects.requireNonNull(SpecialGlyphs.MISSING.bake(this.stitcher));
      this.whiteGlyph = SpecialGlyphs.WHITE.bake(this.stitcher);
   }

   private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> p_328855_, Set<FontOption> p_331640_) {
      IntSet intset = new IntOpenHashSet();
      List<GlyphProvider> list = new ArrayList<>();

      for (GlyphProvider.Conditional glyphprovider$conditional : p_328855_) {
         if (glyphprovider$conditional.filter().apply(p_331640_)) {
            list.add(glyphprovider$conditional.provider());
            intset.addAll(glyphprovider$conditional.provider().getSupportedGlyphs());
         }
      }

      Set<GlyphProvider> set = Sets.newHashSet();
      intset.forEach(
         p_420732_ -> {
            for (GlyphProvider glyphprovider : list) {
               UnbakedGlyph unbakedglyph = glyphprovider.getGlyph(p_420732_);
               if (unbakedglyph != null) {
                  set.add(glyphprovider);
                  if (unbakedglyph.info() != SpecialGlyphs.MISSING) {
                     ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil(unbakedglyph.info().getAdvance(false)), p_232567_ -> new IntArrayList()))
                        .add(p_420732_);
                  }
                  break;
               }
            }
         }
      );
      return list.stream().filter(set::contains).toList();
   }

   @Override
   public void close() {
      this.stitcher.close();
   }

   private static boolean hasFishyAdvance(GlyphInfo p_243323_) {
      float f = p_243323_.getAdvance(false);
      if (!(f < 0.0F) && !(f > 32.0F)) {
         float f1 = p_243323_.getAdvance(true);
         return f1 < 0.0F || f1 > 32.0F;
      } else {
         return true;
      }
   }

   private FontSet.SelectedGlyphs computeGlyphInfo(int p_243321_) {
      FontSet.DelayedBake fontset$delayedbake = null;

      for (GlyphProvider glyphprovider : this.activeProviders) {
         UnbakedGlyph unbakedglyph = glyphprovider.getGlyph(p_243321_);
         if (unbakedglyph != null) {
            if (fontset$delayedbake == null) {
               fontset$delayedbake = new FontSet.DelayedBake(unbakedglyph);
            }

            if (!hasFishyAdvance(unbakedglyph.info())) {
               if (fontset$delayedbake.unbaked == unbakedglyph) {
                  return new FontSet.SelectedGlyphs(fontset$delayedbake, fontset$delayedbake);
               }

               return new FontSet.SelectedGlyphs(fontset$delayedbake, new FontSet.DelayedBake(unbakedglyph));
            }
         }
      }

      return fontset$delayedbake != null ? new FontSet.SelectedGlyphs(fontset$delayedbake, this.missingGlyphGetter) : this.missingSelectedGlyphs;
   }

   FontSet.SelectedGlyphs getGlyph(int p_95079_) {
      return this.glyphCache.computeIfAbsent(p_95079_, this.glyphGetter);
   }

   public BakedGlyph getRandomGlyph(RandomSource p_426508_, int p_425986_) {
      IntList intlist = (IntList)this.glyphsByWidth.get(p_425986_);
      return intlist != null && !intlist.isEmpty() ? this.getGlyph(intlist.getInt(p_426508_.nextInt(intlist.size()))).nonFishy().get() : this.missingGlyph;
   }

   public EffectGlyph whiteGlyph() {
      return Objects.requireNonNull(this.whiteGlyph);
   }

   public GlyphSource source(boolean p_430275_) {
      return p_430275_ ? this.nonFishyGlyphs : this.anyGlyphs;
   }

   @OnlyIn(Dist.CLIENT)
   class DelayedBake implements Supplier<BakedGlyph> {
      final UnbakedGlyph unbaked;
      private @Nullable BakedGlyph baked;

      DelayedBake(final UnbakedGlyph p_427869_) {
         this.unbaked = p_427869_;
      }

      public BakedGlyph get() {
         if (this.baked == null) {
            this.baked = this.unbaked.bake(FontSet.this.wrappedStitcher);
         }

         return this.baked;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SelectedGlyphs(Supplier<BakedGlyph> any, Supplier<BakedGlyph> nonFishy) {
      Supplier<BakedGlyph> select(boolean p_429186_) {
         return p_429186_ ? this.nonFishy : this.any;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Source implements GlyphSource {
      private final boolean filterFishyGlyphs;

      public Source(final boolean p_422853_) {
         this.filterFishyGlyphs = p_422853_;
      }

      @Override
      public BakedGlyph getGlyph(int p_426886_) {
         return FontSet.this.getGlyph(p_426886_).select(this.filterFishyGlyphs).get();
      }

      @Override
      public BakedGlyph getRandomGlyph(RandomSource p_429194_, int p_429086_) {
         return FontSet.this.getRandomGlyph(p_429194_, p_429086_);
      }
   }
}
