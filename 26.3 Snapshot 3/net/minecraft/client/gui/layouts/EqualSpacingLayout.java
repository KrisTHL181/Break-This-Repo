package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.Util;

public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children = new ArrayList<>();
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

   public EqualSpacingLayout(final int width, final int height, final EqualSpacingLayout.Orientation orientation) {
      this(0, 0, width, height, orientation);
   }

   public EqualSpacingLayout(final int x, final int y, final int width, final int height, final EqualSpacingLayout.Orientation orientation) {
      super(x, y, width, height);
      this.orientation = orientation;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int totalChildPrimaryLength = 0;
         int maxChildSecondaryLength = this.orientation.getSecondaryLength(this);

         for (EqualSpacingLayout.ChildContainer child : this.children) {
            totalChildPrimaryLength += this.orientation.getPrimaryLength(child);
            maxChildSecondaryLength = Math.max(maxChildSecondaryLength, this.orientation.getSecondaryLength(child));
         }

         int remainingSpace = this.orientation.getPrimaryLength(this) - totalChildPrimaryLength;
         int position = this.orientation.getPrimaryPosition(this);
         Iterator<EqualSpacingLayout.ChildContainer> childIterator = this.children.iterator();
         EqualSpacingLayout.ChildContainer firstChild = childIterator.next();
         this.orientation.setPrimaryPosition(firstChild, position);
         position += this.orientation.getPrimaryLength(firstChild);
         if (this.children.size() >= 2) {
            Divisor divisor = new Divisor(remainingSpace, this.children.size() - 1);

            while (divisor.hasNext()) {
               position += divisor.nextInt();
               EqualSpacingLayout.ChildContainer child = childIterator.next();
               this.orientation.setPrimaryPosition(child, position);
               position += this.orientation.getPrimaryLength(child);
            }
         }

         int thisSecondaryPosition = this.orientation.getSecondaryPosition(this);

         for (EqualSpacingLayout.ChildContainer child : this.children) {
            this.orientation.setSecondaryPosition(child, thisSecondaryPosition, maxChildSecondaryLength);
         }

         switch (this.orientation) {
            case HORIZONTAL:
               this.height = maxChildSecondaryLength;
               break;
            case VERTICAL:
               this.width = maxChildSecondaryLength;
         }
      }
   }

   @Override
   public void visitChildren(final Consumer<LayoutElement> layoutElementVisitor) {
      this.children.forEach(wrapper -> layoutElementVisitor.accept(wrapper.child));
   }

   @Override
   public void removeChildren() {
      this.children.clear();
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(final T child) {
      return this.addChild(child, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(final T child, final LayoutSettings layoutSettings) {
      this.children.add(new EqualSpacingLayout.ChildContainer(child, layoutSettings));
      return child;
   }

   public <T extends LayoutElement> T addChild(final T child, final Consumer<LayoutSettings> layoutSettingsAdjustments) {
      return this.addChild(child, Util.make(this.newChildLayoutSettings(), layoutSettingsAdjustments));
   }

   private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(final LayoutElement child, final LayoutSettings layoutSettings) {
         super(child, layoutSettings);
      }
   }

   public enum Orientation {
      HORIZONTAL,
      VERTICAL;

      private int getPrimaryLength(final LayoutElement widget) {
         return switch (this) {
            case HORIZONTAL -> widget.getWidth();
            case VERTICAL -> widget.getHeight();
         };
      }

      private int getPrimaryLength(final EqualSpacingLayout.ChildContainer childContainer) {
         return switch (this) {
            case HORIZONTAL -> childContainer.getWidth();
            case VERTICAL -> childContainer.getHeight();
         };
      }

      private int getSecondaryLength(final LayoutElement widget) {
         return switch (this) {
            case HORIZONTAL -> widget.getHeight();
            case VERTICAL -> widget.getWidth();
         };
      }

      private int getSecondaryLength(final EqualSpacingLayout.ChildContainer childContainer) {
         return switch (this) {
            case HORIZONTAL -> childContainer.getHeight();
            case VERTICAL -> childContainer.getWidth();
         };
      }

      private void setPrimaryPosition(final EqualSpacingLayout.ChildContainer childContainer, final int position) {
         switch (this) {
            case HORIZONTAL:
               childContainer.setX(position, childContainer.getWidth());
               break;
            case VERTICAL:
               childContainer.setY(position, childContainer.getHeight());
         }
      }

      private void setSecondaryPosition(final EqualSpacingLayout.ChildContainer childContainer, final int position, final int availableSpace) {
         switch (this) {
            case HORIZONTAL:
               childContainer.setY(position, availableSpace);
               break;
            case VERTICAL:
               childContainer.setX(position, availableSpace);
         }
      }

      private int getPrimaryPosition(final LayoutElement widget) {
         return switch (this) {
            case HORIZONTAL -> widget.getX();
            case VERTICAL -> widget.getY();
         };
      }

      private int getSecondaryPosition(final LayoutElement widget) {
         return switch (this) {
            case HORIZONTAL -> widget.getY();
            case VERTICAL -> widget.getX();
         };
      }
   }
}
