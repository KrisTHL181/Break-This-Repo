package net.minecraft.client.gui.navigation;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ScreenRectangle(ScreenPosition position, int width, int height) {
   private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

   public ScreenRectangle(int p_265721_, int p_265116_, int p_265225_, int p_265493_) {
      this(new ScreenPosition(p_265721_, p_265116_), p_265225_, p_265493_);
   }

   public static ScreenRectangle empty() {
      return EMPTY;
   }

   public static ScreenRectangle of(ScreenAxis p_265648_, int p_265317_, int p_265685_, int p_265218_, int p_265226_) {
      return switch (p_265648_) {
         case HORIZONTAL -> new ScreenRectangle(p_265317_, p_265685_, p_265218_, p_265226_);
         case VERTICAL -> new ScreenRectangle(p_265685_, p_265317_, p_265226_, p_265218_);
      };
   }

   public ScreenRectangle step(ScreenDirection p_265714_) {
      return new ScreenRectangle(this.position.step(p_265714_), this.width, this.height);
   }

   public int getLength(ScreenAxis p_265463_) {
      return switch (p_265463_) {
         case HORIZONTAL -> this.width;
         case VERTICAL -> this.height;
      };
   }

   public int getBoundInDirection(ScreenDirection p_265778_) {
      ScreenAxis screenaxis = p_265778_.getAxis();
      return p_265778_.isPositive() ? this.position.getCoordinate(screenaxis) + this.getLength(screenaxis) - 1 : this.position.getCoordinate(screenaxis);
   }

   public ScreenRectangle getBorder(ScreenDirection p_265704_) {
      int i = this.getBoundInDirection(p_265704_);
      ScreenAxis screenaxis = p_265704_.getAxis().orthogonal();
      int j = this.getBoundInDirection(screenaxis.getNegative());
      int k = this.getLength(screenaxis);
      return of(p_265704_.getAxis(), i, j, 1, k).step(p_265704_);
   }

   public boolean overlaps(ScreenRectangle p_265652_) {
      return this.overlapsInAxis(p_265652_, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(p_265652_, ScreenAxis.VERTICAL);
   }

   public boolean overlapsInAxis(ScreenRectangle p_265306_, ScreenAxis p_265340_) {
      int i = this.getBoundInDirection(p_265340_.getNegative());
      int j = p_265306_.getBoundInDirection(p_265340_.getNegative());
      int k = this.getBoundInDirection(p_265340_.getPositive());
      int l = p_265306_.getBoundInDirection(p_265340_.getPositive());
      return Math.max(i, j) <= Math.min(k, l);
   }

   public int getCenterInAxis(ScreenAxis p_265694_) {
      return (this.getBoundInDirection(p_265694_.getPositive()) + this.getBoundInDirection(p_265694_.getNegative())) / 2;
   }

   public @Nullable ScreenRectangle intersection(ScreenRectangle p_276058_) {
      int i = Math.max(this.left(), p_276058_.left());
      int j = Math.max(this.top(), p_276058_.top());
      int k = Math.min(this.right(), p_276058_.right());
      int l = Math.min(this.bottom(), p_276058_.bottom());
      return i < k && j < l ? new ScreenRectangle(i, j, k - i, l - j) : null;
   }

   public boolean intersects(ScreenRectangle p_410168_) {
      return this.left() < p_410168_.right() && this.right() > p_410168_.left() && this.top() < p_410168_.bottom() && this.bottom() > p_410168_.top();
   }

   public boolean encompasses(ScreenRectangle p_409978_) {
      return p_409978_.left() >= this.left() && p_409978_.top() >= this.top() && p_409978_.right() <= this.right() && p_409978_.bottom() <= this.bottom();
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

   public boolean containsPoint(int p_331100_, int p_333319_) {
      return p_331100_ >= this.left() && p_331100_ < this.right() && p_333319_ >= this.top() && p_333319_ < this.bottom();
   }

   public ScreenRectangle transformAxisAligned(Matrix3x2fc p_450374_) {
      Vector2f vector2f = p_450374_.transformPosition(this.left(), this.top(), new Vector2f());
      Vector2f vector2f1 = p_450374_.transformPosition(this.right(), this.bottom(), new Vector2f());
      return new ScreenRectangle(Mth.floor(vector2f.x), Mth.floor(vector2f.y), Mth.floor(vector2f1.x - vector2f.x), Mth.floor(vector2f1.y - vector2f.y));
   }

   public ScreenRectangle transformMaxBounds(Matrix3x2fc p_453192_) {
      Vector2f vector2f = p_453192_.transformPosition(this.left(), this.top(), new Vector2f());
      Vector2f vector2f1 = p_453192_.transformPosition(this.right(), this.top(), new Vector2f());
      Vector2f vector2f2 = p_453192_.transformPosition(this.left(), this.bottom(), new Vector2f());
      Vector2f vector2f3 = p_453192_.transformPosition(this.right(), this.bottom(), new Vector2f());
      float f = Math.min(Math.min(vector2f.x(), vector2f2.x()), Math.min(vector2f1.x(), vector2f3.x()));
      float f1 = Math.max(Math.max(vector2f.x(), vector2f2.x()), Math.max(vector2f1.x(), vector2f3.x()));
      float f2 = Math.min(Math.min(vector2f.y(), vector2f2.y()), Math.min(vector2f1.y(), vector2f3.y()));
      float f3 = Math.max(Math.max(vector2f.y(), vector2f2.y()), Math.max(vector2f1.y(), vector2f3.y()));
      return new ScreenRectangle(Mth.floor(f), Mth.floor(f2), Mth.ceil(f1 - f), Mth.ceil(f3 - f2));
   }
}
