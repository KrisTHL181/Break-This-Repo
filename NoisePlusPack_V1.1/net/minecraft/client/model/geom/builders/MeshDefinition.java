package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MeshDefinition {
   private final PartDefinition root;

   public MeshDefinition() {
      this(new PartDefinition(ImmutableList.of(), PartPose.ZERO));
   }

   private MeshDefinition(PartDefinition p_366027_) {
      this.root = p_366027_;
   }

   public PartDefinition getRoot() {
      return this.root;
   }

   public MeshDefinition transformed(UnaryOperator<PartPose> p_370109_) {
      return new MeshDefinition(this.root.transformed(p_370109_));
   }

   public MeshDefinition apply(MeshTransformer p_391830_) {
      return p_391830_.apply(this);
   }
}
