package net.minecraft.client.gui;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ActiveTextCollector {
   double PERIOD_PER_SCROLLED_PIXEL = 0.5;
   double MIN_SCROLL_PERIOD = 3.0;

   ActiveTextCollector.Parameters defaultParameters();

   void defaultParameters(ActiveTextCollector.Parameters var1);

   default void accept(int p_457377_, int p_452859_, FormattedCharSequence p_459166_) {
      this.accept(TextAlignment.LEFT, p_457377_, p_452859_, this.defaultParameters(), p_459166_);
   }

   default void accept(int p_460589_, int p_459227_, Component p_450614_) {
      this.accept(TextAlignment.LEFT, p_460589_, p_459227_, this.defaultParameters(), p_450614_.getVisualOrderText());
   }

   default void accept(TextAlignment p_456355_, int p_455654_, int p_452242_, ActiveTextCollector.Parameters p_459659_, Component p_454480_) {
      this.accept(p_456355_, p_455654_, p_452242_, p_459659_, p_454480_.getVisualOrderText());
   }

   void accept(TextAlignment var1, int var2, int var3, ActiveTextCollector.Parameters var4, FormattedCharSequence var5);

   default void accept(TextAlignment p_458304_, int p_452546_, int p_450978_, Component p_453126_) {
      this.accept(p_458304_, p_452546_, p_450978_, p_453126_.getVisualOrderText());
   }

   default void accept(TextAlignment p_452497_, int p_455533_, int p_458604_, FormattedCharSequence p_460212_) {
      this.accept(p_452497_, p_455533_, p_458604_, this.defaultParameters(), p_460212_);
   }

   void acceptScrolling(Component var1, int var2, int var3, int var4, int var5, int var6, ActiveTextCollector.Parameters var7);

   default void acceptScrolling(Component p_455974_, int p_455074_, int p_456961_, int p_456152_, int p_458509_, int p_460914_) {
      this.acceptScrolling(p_455974_, p_455074_, p_456961_, p_456152_, p_458509_, p_460914_, this.defaultParameters());
   }

   default void acceptScrollingWithDefaultCenter(Component p_460248_, int p_456463_, int p_458954_, int p_452485_, int p_455779_) {
      this.acceptScrolling(p_460248_, (p_456463_ + p_458954_) / 2, p_456463_, p_458954_, p_452485_, p_455779_);
   }

   default void defaultScrollingHelper(
      Component p_453331_,
      int p_454829_,
      int p_454529_,
      int p_459959_,
      int p_455153_,
      int p_455707_,
      int p_452736_,
      int p_458940_,
      ActiveTextCollector.Parameters p_457619_
   ) {
      int i = (p_455153_ + p_455707_ - p_458940_) / 2 + 1;
      int j = p_459959_ - p_454529_;
      if (p_452736_ > j) {
         int k = p_452736_ - j;
         double d0 = Util.getMillis() / 1000.0;
         double d1 = Math.max(k * 0.5, 3.0);
         double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
         double d3 = Mth.lerp(d2, 0.0, k);
         ActiveTextCollector.Parameters activetextcollector$parameters = p_457619_.withScissor(p_454529_, p_459959_, p_455153_, p_455707_);
         this.accept(TextAlignment.LEFT, p_454529_ - (int)d3, i, activetextcollector$parameters, p_453331_.getVisualOrderText());
      } else {
         int l = Mth.clamp(p_454829_, p_454529_ + p_452736_ / 2, p_459959_ - p_452736_ / 2);
         this.accept(TextAlignment.CENTER, l, i, p_453331_);
      }
   }

   static void findElementUnderCursor(GuiTextRenderState p_454910_, float p_457481_, float p_457577_, final Consumer<Style> p_456032_) {
      ScreenRectangle screenrectangle = p_454910_.bounds();
      if (screenrectangle != null && screenrectangle.containsPoint((int)p_457481_, (int)p_457577_)) {
         Vector2fc vector2fc = p_454910_.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(p_457481_, p_457577_));
         final float f = vector2fc.x();
         final float f1 = vector2fc.y();
         p_454910_.ensurePrepared()
            .visit(
               new Font.GlyphVisitor() {
                  @Override
                  public void acceptGlyph(TextRenderable.Styled p_456690_) {
                     this.acceptActiveArea(p_456690_);
                  }

                  @Override
                  public void acceptEmptyArea(EmptyArea p_455325_) {
                     this.acceptActiveArea(p_455325_);
                  }

                  private void acceptActiveArea(ActiveArea p_455068_) {
                     if (ActiveTextCollector.isPointInRectangle(
                        f, f1, p_455068_.activeLeft(), p_455068_.activeTop(), p_455068_.activeRight(), p_455068_.activeBottom()
                     )) {
                        p_456032_.accept(p_455068_.style());
                     }
                  }
               }
            );
      }
   }

   static boolean isPointInRectangle(float p_458941_, float p_453323_, float p_456813_, float p_456753_, float p_453879_, float p_459652_) {
      return p_458941_ >= p_456813_ && p_458941_ < p_453879_ && p_453323_ >= p_456753_ && p_453323_ < p_459652_;
   }

   @OnlyIn(Dist.CLIENT)
   class ClickableStyleFinder implements ActiveTextCollector {
      private static final ActiveTextCollector.Parameters INITIAL = new ActiveTextCollector.Parameters(new Matrix3x2f());
      private final Font font;
      private final int testX;
      private final int testY;
      private ActiveTextCollector.Parameters defaultParameters = INITIAL;
      private boolean includeInsertions;
      private @Nullable Style result;
      private final Consumer<Style> styleScanner = p_456200_ -> {
         if (p_456200_.getClickEvent() != null || this.includeInsertions && p_456200_.getInsertion() != null) {
            this.result = p_456200_;
         }
      };

      public ClickableStyleFinder(Font p_453466_, int p_451711_, int p_455853_) {
         this.font = p_453466_;
         this.testX = p_451711_;
         this.testY = p_455853_;
      }

      @Override
      public ActiveTextCollector.Parameters defaultParameters() {
         return this.defaultParameters;
      }

      @Override
      public void defaultParameters(ActiveTextCollector.Parameters p_460443_) {
         this.defaultParameters = p_460443_;
      }

      @Override
      public void accept(TextAlignment p_458143_, int p_451814_, int p_458064_, ActiveTextCollector.Parameters p_460848_, FormattedCharSequence p_453958_) {
         int i = p_458143_.calculateLeft(p_451814_, this.font, p_453958_);
         GuiTextRenderState guitextrenderstate = new GuiTextRenderState(
            this.font, p_453958_, p_460848_.pose(), i, p_458064_, ARGB.white(p_460848_.opacity()), 0, true, true, p_460848_.scissor()
         );
         ActiveTextCollector.findElementUnderCursor(guitextrenderstate, this.testX, this.testY, this.styleScanner);
      }

      @Override
      public void acceptScrolling(
         Component p_459211_, int p_452575_, int p_452952_, int p_460262_, int p_459655_, int p_450826_, ActiveTextCollector.Parameters p_460953_
      ) {
         int i = this.font.width(p_459211_);
         int j = 9;
         this.defaultScrollingHelper(p_459211_, p_452575_, p_452952_, p_460262_, p_459655_, p_450826_, i, j, p_460953_);
      }

      public ActiveTextCollector.ClickableStyleFinder includeInsertions(boolean p_457742_) {
         this.includeInsertions = p_457742_;
         return this;
      }

      public @Nullable Style result() {
         return this.result;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Parameters(Matrix3x2fc pose, float opacity, @Nullable ScreenRectangle scissor) {
      public Parameters(Matrix3x2fc p_452094_) {
         this(p_452094_, 1.0F, null);
      }

      public ActiveTextCollector.Parameters withPose(Matrix3x2fc p_459704_) {
         return new ActiveTextCollector.Parameters(p_459704_, this.opacity, this.scissor);
      }

      public ActiveTextCollector.Parameters withScale(float p_454846_) {
         return this.withPose(this.pose.scale(p_454846_, p_454846_, new Matrix3x2f()));
      }

      public ActiveTextCollector.Parameters withOpacity(float p_457229_) {
         return this.opacity == p_457229_ ? this : new ActiveTextCollector.Parameters(this.pose, p_457229_, this.scissor);
      }

      public ActiveTextCollector.Parameters withScissor(ScreenRectangle p_461031_) {
         return p_461031_.equals(this.scissor) ? this : new ActiveTextCollector.Parameters(this.pose, this.opacity, p_461031_);
      }

      public ActiveTextCollector.Parameters withScissor(int p_459778_, int p_451621_, int p_457080_, int p_460630_) {
         ScreenRectangle screenrectangle = new ScreenRectangle(p_459778_, p_457080_, p_451621_ - p_459778_, p_460630_ - p_457080_)
            .transformAxisAligned(this.pose);
         if (this.scissor != null) {
            screenrectangle = Objects.requireNonNullElse(this.scissor.intersection(screenrectangle), ScreenRectangle.empty());
         }

         return this.withScissor(screenrectangle);
      }
   }
}
