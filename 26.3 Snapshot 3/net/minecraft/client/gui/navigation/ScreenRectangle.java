package net.minecraft.client.gui.navigation;

import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

public record ScreenRectangle(ScreenPosition position, int width, int height) {
   private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

   public ScreenRectangle(final int x, final int y, final int width, final int height) {
      this(new ScreenPosition(x, y), width, height);
   }

   public static ScreenRectangle empty() {
      return EMPTY;
   }

   public static ScreenRectangle of(
      final ScreenAxis primaryAxis, final int primaryIndex, final int secondaryIndex, final int primaryLength, final int secondaryLength
   ) {
      return switch (primaryAxis) {
         case HORIZONTAL -> new ScreenRectangle(primaryIndex, secondaryIndex, primaryLength, secondaryLength);
         case VERTICAL -> new ScreenRectangle(secondaryIndex, primaryIndex, secondaryLength, primaryLength);
      };
   }

   public ScreenRectangle step(final ScreenDirection direction) {
      return new ScreenRectangle(this.position.step(direction), this.width, this.height);
   }

   public int getLength(final ScreenAxis axis) {
      return switch (axis) {
         case HORIZONTAL -> this.width;
         case VERTICAL -> this.height;
      };
   }

   public int getBoundInDirection(final ScreenDirection direction) {
      ScreenAxis axis = direction.getAxis();
      return direction.isPositive() ? this.position.getCoordinate(axis) + this.getLength(axis) - 1 : this.position.getCoordinate(axis);
   }

   public ScreenRectangle getBorder(final ScreenDirection direction) {
      int startFirst = this.getBoundInDirection(direction);
      ScreenAxis orthogonalAxis = direction.getAxis().orthogonal();
      int startSecond = this.getBoundInDirection(orthogonalAxis.getNegative());
      int length = this.getLength(orthogonalAxis);
      return of(direction.getAxis(), startFirst, startSecond, 1, length).step(direction);
   }

   public boolean overlaps(final ScreenRectangle other) {
      return this.overlapsInAxis(other, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(other, ScreenAxis.VERTICAL);
   }

   public boolean overlapsInAxis(final ScreenRectangle other, final ScreenAxis axis) {
      int thisLower = this.getBoundInDirection(axis.getNegative());
      int otherLower = other.getBoundInDirection(axis.getNegative());
      int thisHigher = this.getBoundInDirection(axis.getPositive());
      int otherHigher = other.getBoundInDirection(axis.getPositive());
      return Math.max(thisLower, otherLower) <= Math.min(thisHigher, otherHigher);
   }

   public int getCenterInAxis(final ScreenAxis axis) {
      return (this.getBoundInDirection(axis.getPositive()) + this.getBoundInDirection(axis.getNegative())) / 2;
   }

   public @Nullable ScreenRectangle intersection(final ScreenRectangle other) {
      int left = Math.max(this.left(), other.left());
      int top = Math.max(this.top(), other.top());
      int right = Math.min(this.right(), other.right());
      int bottom = Math.min(this.bottom(), other.bottom());
      return left < right && top < bottom ? new ScreenRectangle(left, top, right - left, bottom - top) : null;
   }

   public boolean intersects(final ScreenRectangle other) {
      return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
   }

   public boolean encompasses(final ScreenRectangle other) {
      return other.left() >= this.left() && other.top() >= this.top() && other.right() <= this.right() && other.bottom() <= this.bottom();
   }

   public int top() {
      return this.position.y();
   }

   public int bottom() {
      return this.position.y() + this.height;
   }

   public int left() {
      return this.position.x();
   }

   public int right() {
      return this.position.x() + this.width;
   }

   public boolean containsPoint(final int x, final int y) {
      return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
   }

   public ScreenRectangle transformAxisAligned(final Matrix3x2fc matrix) {
      Vector2f topLeft = matrix.transformPosition(this.left(), this.top(), new Vector2f());
      Vector2f bottomRight = matrix.transformPosition(this.right(), this.bottom(), new Vector2f());
      return new ScreenRectangle(Mth.floor(topLeft.x), Mth.floor(topLeft.y), Mth.floor(bottomRight.x - topLeft.x), Mth.floor(bottomRight.y - topLeft.y));
   }

   public ScreenRectangle transformMaxBounds(final Matrix3x2fc matrix) {
      Vector2f topLeft = matrix.transformPosition(this.left(), this.top(), new Vector2f());
      Vector2f topRight = matrix.transformPosition(this.right(), this.top(), new Vector2f());
      Vector2f bottomLeft = matrix.transformPosition(this.left(), this.bottom(), new Vector2f());
      Vector2f bottomRight = matrix.transformPosition(this.right(), this.bottom(), new Vector2f());
      float minX = Math.min(Math.min(topLeft.x(), bottomLeft.x()), Math.min(topRight.x(), bottomRight.x()));
      float maxX = Math.max(Math.max(topLeft.x(), bottomLeft.x()), Math.max(topRight.x(), bottomRight.x()));
      float minY = Math.min(Math.min(topLeft.y(), bottomLeft.y()), Math.min(topRight.y(), bottomRight.y()));
      float maxY = Math.max(Math.max(topLeft.y(), bottomLeft.y()), Math.max(topRight.y(), bottomRight.y()));
      return new ScreenRectangle(Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX - minX), Mth.ceil(maxY - minY));
   }
}
