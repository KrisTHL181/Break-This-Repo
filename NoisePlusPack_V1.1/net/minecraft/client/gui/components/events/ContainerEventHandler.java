package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double p_94730_, double p_94731_) {
      for (GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener.isMouseOver(p_94730_, p_94731_)) {
            return Optional.of(guieventlistener);
         }
      }

      return Optional.empty();
   }

   @Override
   default boolean mouseClicked(MouseButtonEvent p_430564_, boolean p_431348_) {
      Optional<GuiEventListener> optional = this.getChildAt(p_430564_.x(), p_430564_.y());
      if (optional.isEmpty()) {
         return false;
      }

      GuiEventListener guieventlistener = optional.get();
      if (guieventlistener.mouseClicked(p_430564_, p_431348_) && guieventlistener.shouldTakeFocusAfterInteraction()) {
         this.setFocused(guieventlistener);
         if (p_430564_.button() == 0) {
            this.setDragging(true);
         }
      }

      return true;
   }

   @Override
   default boolean mouseReleased(MouseButtonEvent p_429390_) {
      if (p_429390_.button() == 0 && this.isDragging()) {
         this.setDragging(false);
         if (this.getFocused() != null) {
            return this.getFocused().mouseReleased(p_429390_);
         }
      }

      return false;
   }

   @Override
   default boolean mouseDragged(MouseButtonEvent p_430509_, double p_94699_, double p_94700_) {
      return this.getFocused() != null && this.isDragging() && p_430509_.button() == 0 ? this.getFocused().mouseDragged(p_430509_, p_94699_, p_94700_) : false;
   }

   boolean isDragging();

   void setDragging(boolean var1);

   @Override
   default boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_, double p_299502_) {
      return this.getChildAt(p_94686_, p_94687_).filter(p_296182_ -> p_296182_.mouseScrolled(p_94686_, p_94687_, p_94688_, p_299502_)).isPresent();
   }

   @Override
   default boolean keyPressed(KeyEvent p_428477_) {
      return this.getFocused() != null && this.getFocused().keyPressed(p_428477_);
   }

   @Override
   default boolean keyReleased(KeyEvent p_431412_) {
      return this.getFocused() != null && this.getFocused().keyReleased(p_431412_);
   }

   @Override
   default boolean charTyped(CharacterEvent p_429843_) {
      return this.getFocused() != null && this.getFocused().charTyped(p_429843_);
   }

   @Nullable GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener var1);

   @Override
   default void setFocused(boolean p_265504_) {
   }

   @Override
   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Override
   default @Nullable ComponentPath getCurrentFocusPath() {
      GuiEventListener guieventlistener = this.getFocused();
      return guieventlistener != null ? ComponentPath.path(this, guieventlistener.getCurrentFocusPath()) : null;
   }

   @Override
   default @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_265668_) {
      GuiEventListener guieventlistener = this.getFocused();
      if (guieventlistener != null) {
         ComponentPath componentpath = guieventlistener.nextFocusPath(p_265668_);
         if (componentpath != null) {
            return ComponentPath.path(this, componentpath);
         }
      }

      if (p_265668_ instanceof FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation) {
         return this.handleTabNavigation(focusnavigationevent$tabnavigation);
      } else {
         return p_265668_ instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation
            ? this.handleArrowNavigation(focusnavigationevent$arrownavigation)
            : null;
      }
   }

   private @Nullable ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation p_265354_) {
      boolean flag = p_265354_.forward();
      GuiEventListener guieventlistener = this.getFocused();
      List<? extends GuiEventListener> list = new ArrayList<>(this.children());
      Collections.sort(list, Comparator.comparingInt(p_447980_ -> p_447980_.getTabOrderGroup()));
      int j = list.indexOf(guieventlistener);
      int i;
      if (guieventlistener != null && j >= 0) {
         i = j + (flag ? 1 : 0);
      } else if (flag) {
         i = 0;
      } else {
         i = list.size();
      }

      ListIterator<? extends GuiEventListener> listiterator = list.listIterator(i);
      BooleanSupplier booleansupplier = flag ? listiterator::hasNext : listiterator::hasPrevious;
      Supplier<? extends GuiEventListener> supplier = flag ? listiterator::next : listiterator::previous;

      while (booleansupplier.getAsBoolean()) {
         GuiEventListener guieventlistener1 = supplier.get();
         ComponentPath componentpath = guieventlistener1.nextFocusPath(p_265354_);
         if (componentpath != null) {
            return ComponentPath.path(this, componentpath);
         }
      }

      return null;
   }

   private @Nullable ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation p_265760_) {
      GuiEventListener guieventlistener = this.getFocused();
      if (guieventlistener == null) {
         ScreenDirection screendirection = p_265760_.direction();
         ScreenRectangle screenrectangle1 = this.getBorderForArrowNavigation(screendirection.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection(screenrectangle1, screendirection, null, p_265760_));
      } else {
         ScreenRectangle screenrectangle = guieventlistener.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection(screenrectangle, p_265760_.direction(), guieventlistener, p_265760_));
      }
   }

   private @Nullable ComponentPath nextFocusPathInDirection(
      ScreenRectangle p_265054_, ScreenDirection p_265167_, @Nullable GuiEventListener p_265476_, FocusNavigationEvent p_265762_
   ) {
      ScreenAxis screenaxis = p_265167_.getAxis();
      ScreenAxis screenaxis1 = screenaxis.orthogonal();
      ScreenDirection screendirection = screenaxis1.getPositive();
      int i = p_265054_.getBoundInDirection(p_265167_.getOpposite());
      List<GuiEventListener> list = new ArrayList<>();

      for (GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener != p_265476_) {
            ScreenRectangle screenrectangle = guieventlistener.getRectangle();
            if (screenrectangle.overlapsInAxis(p_265054_, screenaxis1)) {
               int j = screenrectangle.getBoundInDirection(p_265167_.getOpposite());
               if (p_265167_.isAfter(j, i)) {
                  list.add(guieventlistener);
               } else if (j == i && p_265167_.isAfter(screenrectangle.getBoundInDirection(p_265167_), p_265054_.getBoundInDirection(p_265167_))) {
                  list.add(guieventlistener);
               }
            }
         }
      }

      Comparator<GuiEventListener> comparator = Comparator.comparing(
         p_264674_ -> p_264674_.getRectangle().getBoundInDirection(p_265167_.getOpposite()), p_265167_.coordinateValueComparator()
      );
      Comparator<GuiEventListener> comparator1 = Comparator.comparing(
         p_264676_ -> p_264676_.getRectangle().getBoundInDirection(screendirection.getOpposite()), screendirection.coordinateValueComparator()
      );
      list.sort(comparator.thenComparing(comparator1));

      for (GuiEventListener guieventlistener1 : list) {
         ComponentPath componentpath = guieventlistener1.nextFocusPath(p_265762_);
         if (componentpath != null) {
            return componentpath;
         }
      }

      return this.nextFocusPathVaguelyInDirection(p_265054_, p_265167_, p_265476_, p_265762_);
   }

   private @Nullable ComponentPath nextFocusPathVaguelyInDirection(
      ScreenRectangle p_265390_, ScreenDirection p_265687_, @Nullable GuiEventListener p_265498_, FocusNavigationEvent p_265048_
   ) {
      ScreenAxis screenaxis = p_265687_.getAxis();
      ScreenAxis screenaxis1 = screenaxis.orthogonal();
      List<Pair<GuiEventListener, Long>> list = new ArrayList<>();
      ScreenPosition screenposition = ScreenPosition.of(screenaxis, p_265390_.getBoundInDirection(p_265687_), p_265390_.getCenterInAxis(screenaxis1));

      for (GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener != p_265498_) {
            ScreenRectangle screenrectangle = guieventlistener.getRectangle();
            ScreenPosition screenposition1 = ScreenPosition.of(
               screenaxis, screenrectangle.getBoundInDirection(p_265687_.getOpposite()), screenrectangle.getCenterInAxis(screenaxis1)
            );
            if (p_265687_.isAfter(screenposition1.getCoordinate(screenaxis), screenposition.getCoordinate(screenaxis))) {
               long i = Vector2i.distanceSquared(screenposition.x(), screenposition.y(), screenposition1.x(), screenposition1.y());
               list.add(Pair.of(guieventlistener, i));
            }
         }
      }

      list.sort(Comparator.comparingDouble(Pair::getSecond));

      for (Pair<GuiEventListener, Long> pair : list) {
         ComponentPath componentpath = ((GuiEventListener)pair.getFirst()).nextFocusPath(p_265048_);
         if (componentpath != null) {
            return componentpath;
         }
      }

      return null;
   }
}
