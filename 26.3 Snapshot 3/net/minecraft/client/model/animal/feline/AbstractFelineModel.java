package net.minecraft.client.model.animal.feline;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.FelineRenderState;

public abstract class AbstractFelineModel<S extends FelineRenderState> extends EntityModel<S> {
   protected static final String TAIL_1 = "tail1";
   protected static final String TAIL_2 = "tail2";
   protected final ModelPart leftHindLeg;
   protected final ModelPart rightHindLeg;
   protected final ModelPart leftFrontLeg;
   protected final ModelPart rightFrontLeg;
   protected final ModelPart tail1;
   protected final ModelPart tail2;
   protected final ModelPart head;
   protected final ModelPart body;

   protected AbstractFelineModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.body = root.getChild("body");
      this.tail1 = root.getChild("tail1");
      this.tail2 = root.getChild("tail2");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
   }
}
