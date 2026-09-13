package net.minecraft.world.phys.shapes;

public interface BooleanOp {
   BooleanOp FALSE = (p_82747_, p_82748_) -> false;
   BooleanOp NOT_OR = (p_82744_, p_82745_) -> !p_82744_ && !p_82745_;
   BooleanOp ONLY_SECOND = (p_82741_, p_82742_) -> p_82742_ && !p_82741_;
   BooleanOp NOT_FIRST = (p_82738_, p_82739_) -> !p_82738_;
   BooleanOp ONLY_FIRST = (p_82735_, p_82736_) -> p_82735_ && !p_82736_;
   BooleanOp NOT_SECOND = (p_82732_, p_82733_) -> !p_82733_;
   BooleanOp NOT_SAME = (p_82729_, p_82730_) -> p_82729_ != p_82730_;
   BooleanOp NOT_AND = (p_82726_, p_82727_) -> !p_82726_ || !p_82727_;
   BooleanOp AND = (p_82723_, p_82724_) -> p_82723_ && p_82724_;
   BooleanOp SAME = (p_82720_, p_82721_) -> p_82720_ == p_82721_;
   BooleanOp SECOND = (p_82717_, p_82718_) -> p_82718_;
   BooleanOp CAUSES = (p_82714_, p_82715_) -> !p_82714_ || p_82715_;
   BooleanOp FIRST = (p_82711_, p_82712_) -> p_82711_;
   BooleanOp CAUSED_BY = (p_82708_, p_82709_) -> p_82708_ || !p_82709_;
   BooleanOp OR = (p_82705_, p_82706_) -> p_82705_ || p_82706_;
   BooleanOp TRUE = (p_82699_, p_82700_) -> true;

   boolean apply(boolean var1, boolean var2);
}
