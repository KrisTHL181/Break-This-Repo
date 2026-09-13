package net.minecraft.client.gui.layouts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class FrameLayout extends AbstractLayout {
   private final List<FrameLayout.ChildContainer> children = new ArrayList<>();
   private int minWidth;
   private int minHeight;
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults().align(0.5F, 0.5F);

   public FrameLayout() {
      this(0, 0, 0, 0);
   }

   public FrameLayout(final int minWidth, final int minHeight) {
      this(0, 0, minWidth, minHeight);
   }

   public FrameLayout(final int x, final int y, final int minWidth, final int minHeight) {
      super(x, y, minWidth, minHeight);
      this.setMinDimensions(minWidth, minHeight);
   }

   public FrameLayout setMinDimensions(final int minWidth, final int minHeight) {
      return this.setMinWidth(minWidth).setMinHeight(minHeight);
   }

   public FrameLayout setMinHeight(final int minHeight) {
      this.minHeight = minHeight;
      return this;
   }

   public FrameLayout setMinWidth(final int minWidth) {
      this.minWidth = minWidth;
      return this;
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int resultWidth = this.minWidth;
      int resultHeight = this.minHeight;

      for (FrameLayout.ChildContainer child : this.children) {
         resultWidth = Math.max(resultWidth, child.getWidth());
         resultHeight = Math.max(resultHeight, child.getHeight());
      }

      for (FrameLayout.ChildContainer child : this.children) {
         child.setX(this.getX(), resultWidth);
         child.setY(this.getY(), resultHeight);
      }

      this.width = resultWidth;
      this.height = resultHeight;
   }

   public <T extends LayoutElement> T addChild(final T child) {
      return this.addChild(child, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(final T child, final LayoutSettings childLayoutSettings) {
      this.children.add(new FrameLayout.ChildContainer(child, childLayoutSettings));
      return child;
   }

   public <T extends LayoutElement> T addChild(final T child, final Consumer<LayoutSettings> layoutSettingsAdjustments) {
      return this.addChild(child, Util.make(this.newChildLayoutSettings(), layoutSettingsAdjustments));
   }

   @Override
   public void visitChildren(final Consumer<LayoutElement> layoutElementVisitor) {
      this.children.forEach(wrapper -> layoutElementVisitor.accept(wrapper.child));
   }

   @Override
   public void removeChildren() {
      this.children.clear();
   }

   public static void centerInRectangle(final LayoutElement widget, final int x, final int y, final int width, final int height) {
      alignInRectangle(widget, x, y, width, height, 0.5F, 0.5F);
   }

   public static void centerInRectangle(final LayoutElement widget, final ScreenRectangle rectangle) {
      centerInRectangle(widget, rectangle.position().x(), rectangle.position().y(), rectangle.width(), rectangle.height());
   }

   public static void alignInRectangle(final LayoutElement widget, final ScreenRectangle rectangle, final float alignX, final float alignY) {
      alignInRectangle(widget, rectangle.left(), rectangle.top(), rectangle.width(), rectangle.height(), alignX, alignY);
   }

   public static void alignInRectangle(
      final LayoutElement widget, final int x, final int y, final int width, final int height, final float alignX, final float alignY
   ) {
      alignInDimension(x, width, widget.getWidth(), widget::setX, alignX);
      alignInDimension(y, height, widget.getHeight(), widget::setY, alignY);
   }

   public static void alignInDimension(final int pos, final int length, final int widgetLength, final Consumer<Integer> setWidgetPos, final float align) {
      int offset = (int)Mth.lerp(align, 0.0F, length - widgetLength);
      setWidgetPos.accept(pos + offset);
   }

   private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(final LayoutElement child, final LayoutSettings layoutSettings) {
         super(child, layoutSettings);
      }
   }
}
