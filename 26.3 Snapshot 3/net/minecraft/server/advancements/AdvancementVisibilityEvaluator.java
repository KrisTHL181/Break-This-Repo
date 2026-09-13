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

   private static AdvancementVisibilityEvaluator.VisibilityRule evaluateVisibilityRule(final Advancement advancement, final boolean isDone) {
      Optional<DisplayInfo> display = advancement.display();
      if (display.isEmpty()) {
         return AdvancementVisibilityEvaluator.VisibilityRule.HIDE;
      } else if (isDone) {
         return AdvancementVisibilityEvaluator.VisibilityRule.SHOW;
      } else {
         return display.get().isHidden() ? AdvancementVisibilityEvaluator.VisibilityRule.HIDE : AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE;
      }
   }

   private static boolean evaluateVisiblityForUnfinishedNode(final Stack<AdvancementVisibilityEvaluator.VisibilityRule> ascendants) {
      for (int i = 0; i <= 2; i++) {
         AdvancementVisibilityEvaluator.VisibilityRule visibility = (AdvancementVisibilityEvaluator.VisibilityRule)ascendants.peek(i);
         if (visibility == AdvancementVisibilityEvaluator.VisibilityRule.SHOW) {
            return true;
         }

         if (visibility == AdvancementVisibilityEvaluator.VisibilityRule.HIDE) {
            return false;
         }
      }

      return false;
   }

   private static boolean evaluateVisibility(
      final AdvancementNode node,
      final Stack<AdvancementVisibilityEvaluator.VisibilityRule> ascendants,
      final Predicate<AdvancementNode> isDoneTest,
      final AdvancementVisibilityEvaluator.Output output
   ) {
      boolean isSelfDone = isDoneTest.test(node);
      AdvancementVisibilityEvaluator.VisibilityRule descendantVisibility = evaluateVisibilityRule(node.advancement(), isSelfDone);
      boolean isSelfOrDescendantDone = isSelfDone;
      ascendants.push(descendantVisibility);

      for (AdvancementNode child : node.children()) {
         isSelfOrDescendantDone |= evaluateVisibility(child, ascendants, isDoneTest, output);
      }

      boolean visiblity = isSelfOrDescendantDone || evaluateVisiblityForUnfinishedNode(ascendants);
      ascendants.pop();
      output.accept(node, visiblity);
      return isSelfOrDescendantDone;
   }

   public static void evaluateVisibility(
      final AdvancementNode node, final Predicate<AdvancementNode> isDone, final AdvancementVisibilityEvaluator.Output output
   ) {
      AdvancementNode root = node.root();
      Stack<AdvancementVisibilityEvaluator.VisibilityRule> visibilityStack = new ObjectArrayList();

      for (int i = 0; i <= 2; i++) {
         visibilityStack.push(AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE);
      }

      evaluateVisibility(root, visibilityStack, isDone, output);
   }

   @FunctionalInterface
   public interface Output {
      void accept(AdvancementNode advancement, boolean visible);
   }

   private enum VisibilityRule {
      SHOW,
      HIDE,
      NO_CHANGE;
   }
}
