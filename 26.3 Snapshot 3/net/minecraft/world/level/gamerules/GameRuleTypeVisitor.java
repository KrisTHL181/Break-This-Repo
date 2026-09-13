package net.minecraft.world.level.gamerules;

public interface GameRuleTypeVisitor {
   default <T> void visit(final GameRule<T> gameRule) {
   }

   default void visitBoolean(final GameRule<Boolean> gameRule) {
   }

   default void visitInteger(final GameRule<Integer> gameRule) {
   }
}
