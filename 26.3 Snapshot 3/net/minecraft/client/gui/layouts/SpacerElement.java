package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;

public class SpacerElement implements LayoutElement {
   private int x;
   private int y;
   private final int width;
   private final int height;

   public SpacerElement(final int width, final int height) {
      this(0, 0, width, height);
   }

   public SpacerElement(final int x, final int y, final int width, final int height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public static SpacerElement width(final int width) {
      return new SpacerElement(width, 0);
   }

   public static SpacerElement height(final int height) {
      return new SpacerElement(0, height);
   }

   @Override
   public void setX(final int x) {
      this.x = x;
   }

   @Override
   public void setY(final int y) {
      this.y = y;
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public void visitWidgets(final Consumer<AbstractWidget> widgetVisitor) {
   }
}
