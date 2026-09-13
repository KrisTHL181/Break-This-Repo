package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;

public class MeshDefinition {
   private final PartDefinition root;

   public MeshDefinition() {
      this(new PartDefinition(ImmutableList.of(), PartPose.ZERO));
   }

   private MeshDefinition(final PartDefinition root) {
      this.root = root;
   }

   public PartDefinition getRoot() {
      return this.root;
   }

   public MeshDefinition transformed(final UnaryOperator<PartPose> function) {
      return new MeshDefinition(this.root.transformed(function));
   }

   public MeshDefinition apply(final MeshTransformer transformer) {
      return transformer.apply(this);
   }
}
