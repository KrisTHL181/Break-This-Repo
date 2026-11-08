package net.minecraft.world.level.gamerules;

public interface GameRuleTypeVisitor {
   default <T> void visit(GameRule<T> p_454918_) {
   }

   default void visitBoolean(GameRule<Boolean> p_457388_) {
   }

   default void visitInteger(GameRule<Integer> p_456365_) {
   }
}
