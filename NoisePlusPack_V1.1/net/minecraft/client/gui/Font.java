package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Font {
   private static final float EFFECT_DEPTH = 0.01F;
   private static final float OVER_EFFECT_DEPTH = 0.01F;
   private static final float UNDER_EFFECT_DEPTH = -0.01F;
   public static final float SHADOW_DEPTH = 0.03F;
   public final int lineHeight = 9;
   private final RandomSource random = RandomSource.create();
   final Font.Provider provider;
   private final StringSplitter splitter;

   public Font(Font.Provider p_430972_) {
      this.provider = p_430972_;
      this.splitter = new StringSplitter(
         (p_420690_, p_420691_) -> this.getGlyphSource(p_420691_.getFont()).getGlyph(p_420690_).info().getAdvance(p_420691_.isBold())
      );
   }

   private GlyphSource getGlyphSource(FontDescription p_431437_) {
      return this.provider.glyphs(p_431437_);
   }

   public String bidirectionalShaping(String p_92802_) {
      try {
         Bidi bidi = new Bidi(new ArabicShaping(8).shape(p_92802_), 127);
         bidi.setReorderingMode(0);
         return bidi.writeReordered(2);
      } catch (ArabicShapingException arabicshapingexception) {
         return p_92802_;
      }
   }

   public void drawInBatch(
      String p_272751_,
      float p_272661_,
      float p_273129_,
      int p_273272_,
      boolean p_273209_,
      Matrix4f p_272940_,
      MultiBufferSource p_273017_,
      Font.DisplayMode p_272608_,
      int p_273365_,
      int p_272755_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_272751_, p_272661_, p_273129_, p_273272_, p_273209_, p_273365_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273017_, p_272940_, p_272608_, p_272755_));
   }

   public void drawInBatch(
      Component p_409939_,
      float p_273006_,
      float p_273254_,
      int p_273375_,
      boolean p_273674_,
      Matrix4f p_273525_,
      MultiBufferSource p_272624_,
      Font.DisplayMode p_273418_,
      int p_273330_,
      int p_272981_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_409939_.getVisualOrderText(), p_273006_, p_273254_, p_273375_, p_273674_, false, p_273330_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_272624_, p_273525_, p_273418_, p_272981_));
   }

   public void drawInBatch(
      FormattedCharSequence p_407439_,
      float p_272811_,
      float p_272610_,
      int p_273422_,
      boolean p_273016_,
      Matrix4f p_273443_,
      MultiBufferSource p_273387_,
      Font.DisplayMode p_273551_,
      int p_272706_,
      int p_273114_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_407439_, p_272811_, p_272610_, p_273422_, p_273016_, false, p_272706_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273387_, p_273443_, p_273551_, p_273114_));
   }

   public void drawInBatch8xOutline(
      FormattedCharSequence p_168646_,
      float p_168647_,
      float p_168648_,
      int p_168649_,
      int p_168650_,
      Matrix4f p_254170_,
      MultiBufferSource p_168652_,
      int p_168653_
   ) {
      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, p_168650_, false, false);

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) {
               float[] afloat = new float[]{p_168647_};
               int k = i;
               int l = j;
               p_168646_.accept((p_420698_, p_420699_, p_420700_) -> {
                  boolean flag = p_420699_.isBold();
                  BakedGlyph bakedglyph = this.getGlyph(p_420700_, p_420699_);
                  font$preparedtextbuilder.x = afloat[0] + k * bakedglyph.info().getShadowOffset();
                  font$preparedtextbuilder.y = p_168648_ + l * bakedglyph.info().getShadowOffset();
                  afloat[0] += bakedglyph.info().getAdvance(flag);
                  return font$preparedtextbuilder.accept(p_420698_, p_420699_.withColor(p_168650_), bakedglyph);
               });
            }
         }
      }

      Font.GlyphVisitor font$glyphvisitor = Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.NORMAL, p_168653_);

      for (TextRenderable.Styled textrenderable$styled : font$preparedtextbuilder.glyphs) {
         font$glyphvisitor.acceptGlyph(textrenderable$styled);
      }

      Font.PreparedTextBuilder font$preparedtextbuilder1 = new Font.PreparedTextBuilder(p_168647_, p_168648_, p_168649_, false, true);
      p_168646_.accept(font$preparedtextbuilder1);
      font$preparedtextbuilder1.visit(Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.POLYGON_OFFSET, p_168653_));
   }

   BakedGlyph getGlyph(int p_427433_, Style p_426471_) {
      GlyphSource glyphsource = this.getGlyphSource(p_426471_.getFont());
      BakedGlyph bakedglyph = glyphsource.getGlyph(p_427433_);
      if (p_426471_.isObfuscated() && p_427433_ != 32) {
         int i = Mth.ceil(bakedglyph.info().getAdvance(false));
         bakedglyph = glyphsource.getRandomGlyph(this.random, i);
      }

      return bakedglyph;
   }

   public Font.PreparedText prepareText(String p_409763_, float p_405856_, float p_406377_, int p_406829_, boolean p_408402_, int p_406561_) {
      if (this.isBidirectional()) {
         p_409763_ = this.bidirectionalShaping(p_409763_);
      }

      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_405856_, p_406377_, p_406829_, p_406561_, p_408402_, false);
      StringDecomposer.iterateFormatted(p_409763_, Style.EMPTY, font$preparedtextbuilder);
      return font$preparedtextbuilder;
   }

   public Font.PreparedText prepareText(
      FormattedCharSequence p_406646_, float p_410379_, float p_409318_, int p_410317_, boolean p_406084_, boolean p_454438_, int p_406668_
   ) {
      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_410379_, p_409318_, p_410317_, p_406668_, p_406084_, p_454438_);
      p_406646_.accept(font$preparedtextbuilder);
      return font$preparedtextbuilder;
   }

   public int width(String p_92896_) {
      return Mth.ceil(this.splitter.stringWidth(p_92896_));
   }

   public int width(FormattedText p_92853_) {
      return Mth.ceil(this.splitter.stringWidth(p_92853_));
   }

   public int width(FormattedCharSequence p_92725_) {
      return Mth.ceil(this.splitter.stringWidth(p_92725_));
   }

   public String plainSubstrByWidth(String p_92838_, int p_92839_, boolean p_92840_) {
      return p_92840_ ? this.splitter.plainTailByWidth(p_92838_, p_92839_, Style.EMPTY) : this.splitter.plainHeadByWidth(p_92838_, p_92839_, Style.EMPTY);
   }

   public String plainSubstrByWidth(String p_92835_, int p_92836_) {
      return this.splitter.plainHeadByWidth(p_92835_, p_92836_, Style.EMPTY);
   }

   public FormattedText substrByWidth(FormattedText p_92855_, int p_92856_) {
      return this.splitter.headByWidth(p_92855_, p_92856_, Style.EMPTY);
   }

   public int wordWrapHeight(FormattedText p_239134_, int p_239135_) {
      return 9 * this.splitter.splitLines(p_239134_, p_239135_, Style.EMPTY).size();
   }

   public List<FormattedCharSequence> split(FormattedText p_92924_, int p_92925_) {
      return Language.getInstance().getVisualOrder(this.splitter.splitLines(p_92924_, p_92925_, Style.EMPTY));
   }

   public List<FormattedText> splitIgnoringLanguage(FormattedText p_407108_, int p_408991_) {
      return this.splitter.splitLines(p_407108_, p_408991_, Style.EMPTY);
   }

   public boolean isBidirectional() {
      return Language.getInstance().isDefaultRightToLeft();
   }

   public StringSplitter getSplitter() {
      return this.splitter;
   }

   @OnlyIn(Dist.CLIENT)
   public enum DisplayMode {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }

   @OnlyIn(Dist.CLIENT)
   public interface GlyphVisitor {
      static Font.GlyphVisitor forMultiBufferSource(
         final MultiBufferSource p_409617_, final Matrix4f p_408497_, final Font.DisplayMode p_409313_, final int p_408611_
      ) {
         return new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled p_460976_) {
               this.render(p_460976_);
            }

            @Override
            public void acceptEffect(TextRenderable p_431653_) {
               this.render(p_431653_);
            }

            private void render(TextRenderable p_423344_) {
               VertexConsumer vertexconsumer = p_409617_.getBuffer(p_423344_.renderType(p_409313_));
               p_423344_.render(p_408497_, vertexconsumer, p_408611_, false);
            }
         };
      }

      default void acceptGlyph(TextRenderable.Styled p_452010_) {
      }

      default void acceptEffect(TextRenderable p_430311_) {
      }

      default void acceptEmptyArea(EmptyArea p_455806_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface PreparedText {
      void visit(Font.GlyphVisitor var1);

      @Nullable ScreenRectangle bounds();
   }

   @OnlyIn(Dist.CLIENT)
   class PreparedTextBuilder implements FormattedCharSink, Font.PreparedText {
      private final boolean drawShadow;
      private final int color;
      private final int backgroundColor;
      private final boolean includeEmpty;
      float x;
      float y;
      private float left = Float.MAX_VALUE;
      private float top = Float.MAX_VALUE;
      private float right = -Float.MAX_VALUE;
      private float bottom = -Float.MAX_VALUE;
      private float backgroundLeft = Float.MAX_VALUE;
      private float backgroundTop = Float.MAX_VALUE;
      private float backgroundRight = -Float.MAX_VALUE;
      private float backgroundBottom = -Float.MAX_VALUE;
      final List<TextRenderable.Styled> glyphs = new ArrayList<>();
      private @Nullable List<TextRenderable> effects;
      private @Nullable List<EmptyArea> emptyAreas;

      public PreparedTextBuilder(final float p_407746_, final float p_410668_, final int p_408037_, final boolean p_406887_, final boolean p_457700_) {
         this(p_407746_, p_410668_, p_408037_, 0, p_406887_, p_457700_);
      }

      public PreparedTextBuilder(
         final float p_408474_, final float p_405862_, final int p_406916_, final int p_407483_, final boolean p_410641_, final boolean p_456735_
      ) {
         this.x = p_408474_;
         this.y = p_405862_;
         this.drawShadow = p_410641_;
         this.color = p_406916_;
         this.backgroundColor = p_407483_;
         this.includeEmpty = p_456735_;
         this.markBackground(p_408474_, p_405862_, 0.0F);
      }

      private void markSize(float p_408328_, float p_410584_, float p_407096_, float p_407028_) {
         this.left = Math.min(this.left, p_408328_);
         this.top = Math.min(this.top, p_410584_);
         this.right = Math.max(this.right, p_407096_);
         this.bottom = Math.max(this.bottom, p_407028_);
      }

      private void markBackground(float p_407445_, float p_408838_, float p_406374_) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            this.backgroundLeft = Math.min(this.backgroundLeft, p_407445_ - 1.0F);
            this.backgroundTop = Math.min(this.backgroundTop, p_408838_ - 1.0F);
            this.backgroundRight = Math.max(this.backgroundRight, p_407445_ + p_406374_);
            this.backgroundBottom = Math.max(this.backgroundBottom, p_408838_ + 9.0F);
            this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
         }
      }

      private void addGlyph(TextRenderable.Styled p_455499_) {
         this.glyphs.add(p_455499_);
         this.markSize(p_455499_.left(), p_455499_.top(), p_455499_.right(), p_455499_.bottom());
      }

      private void addEffect(TextRenderable p_430927_) {
         if (this.effects == null) {
            this.effects = new ArrayList<>();
         }

         this.effects.add(p_430927_);
         this.markSize(p_430927_.left(), p_430927_.top(), p_430927_.right(), p_430927_.bottom());
      }

      private void addEmptyGlyph(EmptyArea p_457914_) {
         if (this.emptyAreas == null) {
            this.emptyAreas = new ArrayList<>();
         }

         this.emptyAreas.add(p_457914_);
      }

      @Override
      public boolean accept(int p_408106_, Style p_408632_, int p_410483_) {
         BakedGlyph bakedglyph = Font.this.getGlyph(p_410483_, p_408632_);
         return this.accept(p_408106_, p_408632_, bakedglyph);
      }

      public boolean accept(int p_422572_, Style p_423260_, BakedGlyph p_426975_) {
         GlyphInfo glyphinfo = p_426975_.info();
         boolean flag = p_423260_.isBold();
         TextColor textcolor = p_423260_.getColor();
         int i = this.getTextColor(textcolor);
         int j = this.getShadowColor(p_423260_, i);
         float f = glyphinfo.getAdvance(flag);
         float f1 = p_422572_ == 0 ? this.x - 1.0F : this.x;
         float f2 = glyphinfo.getShadowOffset();
         float f3 = flag ? glyphinfo.getBoldOffset() : 0.0F;
         TextRenderable.Styled textrenderable$styled = p_426975_.createGlyph(this.x, this.y, i, j, p_423260_, f3, f2);
         if (textrenderable$styled != null) {
            this.addGlyph(textrenderable$styled);
         } else if (this.includeEmpty) {
            this.addEmptyGlyph(new EmptyArea(this.x, this.y, f, 7.0F, 9.0F, p_423260_));
         }

         this.markBackground(this.x, this.y, f);
         if (p_423260_.isStrikethrough()) {
            this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 4.5F - 1.0F, this.x + f, this.y + 4.5F, 0.01F, i, j, f2));
         }

         if (p_423260_.isUnderlined()) {
            this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 9.0F - 1.0F, this.x + f, this.y + 9.0F, 0.01F, i, j, f2));
         }

         this.x += f;
         return true;
      }

      @Override
      public void visit(Font.GlyphVisitor p_407346_) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            p_407346_.acceptEffect(
               Font.this.provider
                  .effect()
                  .createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F)
            );
         }

         for (TextRenderable.Styled textrenderable$styled : this.glyphs) {
            p_407346_.acceptGlyph(textrenderable$styled);
         }

         if (this.effects != null) {
            for (TextRenderable textrenderable : this.effects) {
               p_407346_.acceptEffect(textrenderable);
            }
         }

         if (this.emptyAreas != null) {
            for (EmptyArea emptyarea : this.emptyAreas) {
               p_407346_.acceptEmptyArea(emptyarea);
            }
         }
      }

      private int getTextColor(@Nullable TextColor p_407859_) {
         if (p_407859_ != null) {
            int i = ARGB.alpha(this.color);
            int j = p_407859_.getValue();
            return ARGB.color(i, j);
         } else {
            return this.color;
         }
      }

      private int getShadowColor(Style p_408920_, int p_408082_) {
         Integer integer = p_408920_.getShadowColor();
         if (integer != null) {
            float f = ARGB.alphaFloat(p_408082_);
            float f1 = ARGB.alphaFloat(integer);
            return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
         } else {
            return this.drawShadow ? ARGB.scaleRGB(p_408082_, 0.25F) : 0;
         }
      }

      @Override
      public @Nullable ScreenRectangle bounds() {
         if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
            int i = Mth.floor(this.left);
            int j = Mth.floor(this.top);
            int k = Mth.ceil(this.right);
            int l = Mth.ceil(this.bottom);
            return new ScreenRectangle(i, j, k - i, l - j);
         } else {
            return null;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Provider {
      GlyphSource glyphs(FontDescription var1);

      EffectGlyph effect();
   }
}
