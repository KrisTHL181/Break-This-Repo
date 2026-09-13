package net.minecraft.world.phys.shapes;

public interface BooleanOp {
   BooleanOp FALSE = (first, second) -> false;
   BooleanOp NOT_OR = (first, second) -> !first && !second;
   BooleanOp ONLY_SECOND = (first, second) -> second && !first;
   BooleanOp NOT_FIRST = (first, second) -> !first;
   BooleanOp ONLY_FIRST = (first, second) -> first && !second;
   BooleanOp NOT_SECOND = (first, second) -> !second;
   BooleanOp NOT_SAME = (first, second) -> first != second;
   BooleanOp NOT_AND = (first, second) -> !first || !second;
   BooleanOp AND = (first, second) -> first && second;
   BooleanOp SAME = (first, second) -> first == second;
   BooleanOp SECOND = (first, second) -> second;
   BooleanOp CAUSES = (first, second) -> !first || second;
   BooleanOp FIRST = (first, second) -> first;
   BooleanOp CAUSED_BY = (first, second) -> first || !second;
   BooleanOp OR = (first, second) -> first || second;
   BooleanOp TRUE = (first, second) -> true;

   boolean apply(final boolean first, final boolean second);
}
