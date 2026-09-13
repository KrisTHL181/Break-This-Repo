package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.locale.Language;
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
import org.jspecify.annotations.Nullable;

public class Font {
   private static final float EFFECT_DEPTH = 0.01F;
   private static final float OVER_EFFECT_DEPTH = 0.01F;
   private static final float UNDER_EFFECT_DEPTH = -0.01F;
   public static final float SHADOW_DEPTH = 0.03F;
   public final int lineHeight = 9;
   private final RandomSource random = RandomSource.create();
   private final Font.Provider provider;
   private final StringSplitter splitter;

   public Font(final Font.Provider provider) {
      this.provider = provider;
      this.splitter = new StringSplitter((codepoint, style) -> this.getGlyphSource(style.getFont()).getGlyph(codepoint).info().getAdvance(style.isBold()));
   }

   private GlyphSource getGlyphSource(final FontDescription fontLocation) {
      return this.provider.glyphs(fontLocation);
   }

   public String bidirectionalShaping(final String text) {
      try {
         Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
         bidi.setReorderingMode(0);
         return bidi.writeReordered(2);
      } catch (ArabicShapingException var3) {
         return text;
      }
   }

   public Font.PreparedText prepare8xTextOutline(final FormattedCharSequence str, final float x, final float y, final int outlineColor) {
      Font.PreparedTextBuilder outlineOutput = new Font.PreparedTextBuilder(0.0F, 0.0F, outlineColor, false, false);

      for (int xo = -1; xo <= 1; xo++) {
         for (int yo = -1; yo <= 1; yo++) {
            if (xo != 0 || yo != 0) {
               float[] startX = new float[]{x};
               int finalXo = xo;
               int finalYo = yo;
               str.accept((position, style, codepoint) -> {
                  boolean bold = style.isBold();
                  BakedGlyph glyph = this.getGlyph(codepoint, style);
                  outlineOutput.x = startX[0] + finalXo * glyph.info().getShadowOffset();
                  outlineOutput.y = y + finalYo * glyph.info().getShadowOffset();
                  startX[0] += glyph.info().getAdvance(bold);
                  return outlineOutput.accept(position, style.withColor(outlineColor), glyph);
               });
            }
         }
      }

      outlineOutput.discardEffects();
      return outlineOutput;
   }

   private BakedGlyph getGlyph(final int codepoint, final Style style) {
      GlyphSource glyphSource = this.getGlyphSource(style.getFont());
      BakedGlyph glyph = glyphSource.getGlyph(codepoint);
      if (style.isObfuscated() && codepoint != 32) {
         int targetWidth = Mth.ceil(glyph.info().getAdvance(false));
         glyph = glyphSource.getRandomGlyph(this.random, targetWidth);
      }

      return glyph;
   }

   public Font.PreparedText prepareText(String text, final float x, final float y, final int originalColor, final boolean drawShadow, final int backgroundColor) {
      if (this.isBidirectional()) {
         text = this.bidirectionalShaping(text);
      }

      Font.PreparedTextBuilder output = new Font.PreparedTextBuilder(x, y, originalColor, backgroundColor, drawShadow, false);
      StringDecomposer.iterateFormatted(text, Style.EMPTY, output);
      return output;
   }

   public Font.PreparedText prepareText(
      final FormattedCharSequence text,
      final float x,
      final float y,
      final int originalColor,
      final boolean drawShadow,
      final boolean includeEmpty,
      final int backgroundColor
   ) {
      Font.PreparedTextBuilder builder = new Font.PreparedTextBuilder(x, y, originalColor, backgroundColor, drawShadow, includeEmpty);
      text.accept(builder);
      return builder;
   }

   public int width(final String str) {
      return Mth.ceil(this.splitter.stringWidth(str));
   }

   public int width(final FormattedText text) {
      return Mth.ceil(this.splitter.stringWidth(text));
   }

   public int width(final FormattedCharSequence text) {
      return Mth.ceil(this.splitter.stringWidth(text));
   }

   public String plainSubstrByWidth(final String str, final int width, final boolean reverse) {
      return reverse ? this.splitter.plainTailByWidth(str, width, Style.EMPTY) : this.splitter.plainHeadByWidth(str, width, Style.EMPTY);
   }

   public String plainSubstrByWidth(final String str, final int width) {
      return this.splitter.plainHeadByWidth(str, width, Style.EMPTY);
   }

   public FormattedText substrByWidth(final FormattedText text, final int width) {
      return this.splitter.headByWidth(text, width, Style.EMPTY);
   }

   public int wordWrapHeight(final FormattedText input, final int textWidth) {
      return 9 * this.splitter.splitLines(input, textWidth, Style.EMPTY).size();
   }

   public List<FormattedCharSequence> split(final FormattedText input, final int maxWidth) {
      return Language.getInstance().getVisualOrder(this.splitter.splitLines(input, maxWidth, Style.EMPTY));
   }

   public List<FormattedText> splitIgnoringLanguage(final FormattedText input, final int maxWidth) {
      return this.splitter.splitLines(input, maxWidth, Style.EMPTY);
   }

   public boolean isBidirectional() {
      return Language.getInstance().isDefaultRightToLeft();
   }

   public StringSplitter getSplitter() {
      return this.splitter;
   }

   public enum DisplayMode {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }

   public interface GlyphVisitor {
      default void acceptGlyph(final TextRenderable.Styled glyph) {
         this.acceptRenderable(glyph);
      }

      default void acceptEffect(final TextRenderable effect) {
         this.acceptRenderable(effect);
      }

      default void acceptRenderable(final TextRenderable renderable) {
      }

      default void acceptEmptyArea(final EmptyArea empty) {
      }
   }

   public interface PreparedText {
      void visit(Font.GlyphVisitor visitor);

      @Nullable ScreenRectangle bounds();
   }

   private class PreparedTextBuilder implements Font.PreparedText, FormattedCharSink {
      private final boolean drawShadow;
      private final int color;
      private final int backgroundColor;
      private final boolean includeEmpty;
      private float x;
      private float y;
      private float left = Float.MAX_VALUE;
      private float top = Float.MAX_VALUE;
      private float right = -Float.MAX_VALUE;
      private float bottom = -Float.MAX_VALUE;
      private float backgroundLeft = Float.MAX_VALUE;
      private float backgroundTop = Float.MAX_VALUE;
      private float backgroundRight = -Float.MAX_VALUE;
      private float backgroundBottom = -Float.MAX_VALUE;
      private final List<TextRenderable.Styled> glyphs = new ArrayList<>();
      private @Nullable List<TextRenderable> effects;
      private @Nullable List<EmptyArea> emptyAreas;

      public PreparedTextBuilder(final float x, final float y, final int color, final boolean drawShadow, final boolean includeEmpty) {
         this(x, y, color, 0, drawShadow, includeEmpty);
      }

      public PreparedTextBuilder(final float x, final float y, final int color, final int backgroundColor, final boolean drawShadow, final boolean includeEmpty) {
         this.x = x;
         this.y = y;
         this.drawShadow = drawShadow;
         this.color = color;
         this.backgroundColor = backgroundColor;
         this.includeEmpty = includeEmpty;
         this.markBackground(x, y, 0.0F);
      }

      private void markSize(final float left, final float top, final float right, final float bottom) {
         this.left = Math.min(this.left, left);
         this.top = Math.min(this.top, top);
         this.right = Math.max(this.right, right);
         this.bottom = Math.max(this.bottom, bottom);
      }

      private void markBackground(final float x, final float y, final float advance) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            this.backgroundLeft = Math.min(this.backgroundLeft, x - 1.0F);
            this.backgroundTop = Math.min(this.backgroundTop, y - 1.0F);
            this.backgroundRight = Math.max(this.backgroundRight, x + advance);
            this.backgroundBottom = Math.max(this.backgroundBottom, y + 9.0F);
            this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
         }
      }

      private void addGlyph(final TextRenderable.Styled instance) {
         this.glyphs.add(instance);
         this.markSize(instance.left(), instance.top(), instance.right(), instance.bottom());
      }

      private void addEffect(final TextRenderable effect) {
         if (this.effects == null) {
            this.effects = new ArrayList<>();
         }

         this.effects.add(effect);
         this.markSize(effect.left(), effect.top(), effect.right(), effect.bottom());
      }

      private void addEmptyGlyph(final EmptyArea empty) {
         if (this.emptyAreas == null) {
            this.emptyAreas = new ArrayList<>();
         }

         this.emptyAreas.add(empty);
      }

      @Override
      public boolean accept(final int position, final Style style, final int c) {
         BakedGlyph glyph = Font.this.getGlyph(c, style);
         return this.accept(position, style, glyph);
      }

      public boolean accept(final int position, final Style style, final BakedGlyph glyph) {
         GlyphInfo glyphInfo = glyph.info();
         boolean bold = style.isBold();
         TextColor styleColor = style.getColor();
         int textColor = this.getTextColor(styleColor);
         int shadowColor = this.getShadowColor(style, textColor);
         float advance = glyphInfo.getAdvance(bold);
         float effectX0 = position == 0 ? this.x - 1.0F : this.x;
         float shadowOffset = glyphInfo.getShadowOffset();
         float boldOffset = bold ? glyphInfo.getBoldOffset() : 0.0F;
         TextRenderable.Styled instance = glyph.createGlyph(this.x, this.y, textColor, shadowColor, style, boldOffset, shadowOffset);
         if (instance != null) {
            this.addGlyph(instance);
         } else if (this.includeEmpty) {
            this.addEmptyGlyph(new EmptyArea(this.x, this.y, advance, 7.0F, 9.0F, style));
         }

         this.markBackground(this.x, this.y, advance);
         if (style.isStrikethrough()) {
            this.addEffect(
               Font.this.provider
                  .effect()
                  .createEffect(effectX0, this.y + 4.5F - 1.0F, this.x + advance, this.y + 4.5F, 0.01F, textColor, shadowColor, shadowOffset)
            );
         }

         if (style.isUnderlined()) {
            this.addEffect(
               Font.this.provider
                  .effect()
                  .createEffect(effectX0, this.y + 9.0F - 1.0F, this.x + advance, this.y + 9.0F, 0.01F, textColor, shadowColor, shadowOffset)
            );
         }

         this.x += advance;
         return true;
      }

      @Override
      public void visit(final Font.GlyphVisitor visitor) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            visitor.acceptEffect(
               Font.this.provider
                  .effect()
                  .createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F)
            );
         }

         for (TextRenderable.Styled glyph : this.glyphs) {
            visitor.acceptGlyph(glyph);
         }

         if (this.effects != null) {
            for (TextRenderable effect : this.effects) {
               visitor.acceptEffect(effect);
            }
         }

         if (this.emptyAreas != null) {
            for (EmptyArea emptyArea : this.emptyAreas) {
               visitor.acceptEmptyArea(emptyArea);
            }
         }
      }

      public void discardEffects() {
         this.effects = null;
      }

      private int getTextColor(final @Nullable TextColor textColor) {
         if (textColor != null) {
            int alpha = ARGB.alpha(this.color);
            int rgb = textColor.getValue();
            return ARGB.color(alpha, rgb);
         } else {
            return this.color;
         }
      }

      private int getShadowColor(final Style style, final int textColor) {
         Integer shadow = style.getShadowColor();
         if (shadow != null) {
            float textAlpha = ARGB.alphaFloat(textColor);
            float shadowAlpha = ARGB.alphaFloat(shadow);
            return textAlpha != 1.0F ? ARGB.color(ARGB.as8BitChannel(textAlpha * shadowAlpha), shadow) : shadow;
         } else {
            return this.drawShadow ? ARGB.scaleRGB(textColor, 0.25F) : 0;
         }
      }

      @Override
      public @Nullable ScreenRectangle bounds() {
         if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
            int left = Mth.floor(this.left);
            int top = Mth.floor(this.top);
            int right = Mth.ceil(this.right);
            int bottom = Mth.ceil(this.bottom);
            return new ScreenRectangle(left, top, right - left, bottom - top);
         } else {
            return null;
         }
      }
   }

   public interface Provider {
      GlyphSource glyphs(FontDescription font);

      EffectGlyph effect();
   }
}
