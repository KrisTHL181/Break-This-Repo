package net.minecraft.server.advancements;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;

public class AdvancementVisibilityEvaluator {
   private static final int VISIBILITY_DEPTH = 2;

   private static AdvancementVisibilityEvaluator.VisibilityRule evaluateVisibilityRule(Advancement p_265736_, boolean p_265426_) {
      Optional<DisplayInfo> optional = p_265736_.display();
      if (optional.isEmpty()) {
         return AdvancementVisibilityEvaluator.VisibilityRule.HIDE;
      } else if (p_265426_) {
         return AdvancementVisibilityEvaluator.VisibilityRule.SHOW;
      } else {
         return optional.get().isHidden() ? AdvancementVisibilityEvaluator.VisibilityRule.HIDE : AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE;
      }
   }

   private static boolean evaluateVisiblityForUnfinishedNode(Stack<AdvancementVisibilityEvaluator.VisibilityRule> p_265343_) {
      for (int i = 0; i <= 2; i++) {
         AdvancementVisibilityEvaluator.VisibilityRule advancementvisibilityevaluator$visibilityrule = (AdvancementVisibilityEvaluator.VisibilityRule)p_265343_.peek(
            i
         );
         if (advancementvisibilityevaluator$visibilityrule == AdvancementVisibilityEvaluator.VisibilityRule.SHOW) {
            return true;
         }

         if (advancementvisibilityevaluator$visibilityrule == AdvancementVisibilityEvaluator.VisibilityRule.HIDE) {
            return false;
         }
      }

      return false;
   }

   private static boolean evaluateVisibility(
      AdvancementNode p_299221_,
      Stack<AdvancementVisibilityEvaluator.VisibilityRule> p_298849_,
      Predicate<AdvancementNode> p_265359_,
      AdvancementVisibilityEvaluator.Output p_265303_
   ) {
      boolean flag = p_265359_.test(p_299221_);
      AdvancementVisibilityEvaluator.VisibilityRule advancementvisibilityevaluator$visibilityrule = evaluateVisibilityRule(p_299221_.advancement(), flag);
      boolean flag1 = flag;
      p_298849_.push(advancementvisibilityevaluator$visibilityrule);

      for (AdvancementNode advancementnode : p_299221_.children()) {
         flag1 |= evaluateVisibility(advancementnode, p_298849_, p_265359_, p_265303_);
      }

      boolean flag2 = flag1 || evaluateVisiblityForUnfinishedNode(p_298849_);
      p_298849_.pop();
      p_265303_.accept(p_299221_, flag2);
      return flag1;
   }

   public static void evaluateVisibility(AdvancementNode p_297454_, Predicate<AdvancementNode> p_265561_, AdvancementVisibilityEvaluator.Output p_265381_) {
      AdvancementNode advancementnode = p_297454_.root();
      Stack<AdvancementVisibilityEvaluator.VisibilityRule> stack = new ObjectArrayList();

      for (int i = 0; i <= 2; i++) {
         stack.push(AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE);
      }

      evaluateVisibility(advancementnode, stack, p_265561_, p_265381_);
   }

   @FunctionalInterface
   public interface Output {
      void accept(AdvancementNode var1, boolean var2);
   }

   enum VisibilityRule {
      SHOW,
      HIDE,
      NO_CHANGE;
   }
}
