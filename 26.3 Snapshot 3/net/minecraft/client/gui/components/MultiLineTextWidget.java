package net.minecraft.client.gui.components;

import java.util.OptionalInt;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.Util;

public class MultiLineTextWidget extends AbstractStringWidget {
   private OptionalInt maxWidth = OptionalInt.empty();
   private OptionalInt maxRows = OptionalInt.empty();
   private final SingleKeyCache<MultiLineTextWidget.CacheKey, MultiLineLabel> cache;
   private boolean centered = false;

   public MultiLineTextWidget(final Component message, final Font font) {
      this(0, 0, message, font);
   }

   public MultiLineTextWidget(final int x, final int y, final Component message, final Font font) {
      super(x, y, 0, 0, message, font);
      this.cache = Util.singleKeyCache(
         key -> key.maxRows.isPresent()
            ? MultiLineLabel.create(font, key.maxWidth, key.maxRows.getAsInt(), key.message)
            : MultiLineLabel.create(font, key.message, key.maxWidth)
      );
      this.active = false;
   }

   public MultiLineTextWidget setMaxWidth(final int maxWidth) {
      this.maxWidth = OptionalInt.of(maxWidth);
      return this;
   }

   public MultiLineTextWidget setMaxRows(final int maxRows) {
      this.maxRows = OptionalInt.of(maxRows);
      return this;
   }

   public MultiLineTextWidget setCentered(final boolean centered) {
      this.centered = centered;
      return this;
   }

   @Override
   public int getWidth() {
      return this.cache.getValue(this.getFreshCacheKey()).getWidth();
   }

   @Override
   public int getHeight() {
      return this.cache.getValue(this.getFreshCacheKey()).getLineCount() * 9;
   }

   @Override
   public void visitLines(final ActiveTextCollector output) {
      MultiLineLabel multilineLabel = this.cache.getValue(this.getFreshCacheKey());
      int x = this.getTextX();
      int y = this.getTextY();
      int lineHeight = 9;
      if (this.centered) {
         int midX = this.getX() + this.getWidth() / 2;
         multilineLabel.visitLines(TextAlignment.CENTER, midX, y, lineHeight, output);
      } else {
         multilineLabel.visitLines(TextAlignment.LEFT, x, y, lineHeight, output);
      }
   }

   protected int getTextX() {
      return this.getX();
   }

   protected int getTextY() {
      return this.getY();
   }

   private MultiLineTextWidget.CacheKey getFreshCacheKey() {
      return new MultiLineTextWidget.CacheKey(this.getMessage(), this.maxWidth.orElse(Integer.MAX_VALUE), this.maxRows);
   }

   private record CacheKey(Component message, int maxWidth, OptionalInt maxRows) {
   }
}
