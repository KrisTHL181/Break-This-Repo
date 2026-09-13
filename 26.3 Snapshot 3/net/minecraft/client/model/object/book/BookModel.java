package net.minecraft.client.model.object.book;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public class BookModel extends Model<BookModel.State> {
   private static final String LEFT_PAGES = "left_pages";
   private static final String RIGHT_PAGES = "right_pages";
   private static final String FLIP_PAGE_1 = "flip_page1";
   private static final String FLIP_PAGE_2 = "flip_page2";
   private final ModelPart leftLid;
   private final ModelPart rightLid;
   private final ModelPart leftPages;
   private final ModelPart rightPages;
   private final ModelPart flipPage1;
   private final ModelPart flipPage2;

   public BookModel(final ModelPart root) {
      super(root, RenderTypes::entitySolid);
      this.leftLid = root.getChild("left_lid");
      this.rightLid = root.getChild("right_lid");
      this.leftPages = root.getChild("left_pages");
      this.rightPages = root.getChild("right_pages");
      this.flipPage1 = root.getChild("flip_page1");
      this.flipPage2 = root.getChild("flip_page2");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "left_lid", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), PartPose.offset(0.0F, 0.0F, -1.0F)
      );
      root.addOrReplaceChild(
         "right_lid", CubeListBuilder.create().texOffs(16, 0).addBox(0.0F, -5.0F, -0.005F, 6.0F, 10.0F, 0.005F), PartPose.offset(0.0F, 0.0F, 1.0F)
      );
      root.addOrReplaceChild(
         "seam", CubeListBuilder.create().texOffs(12, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 0.005F), PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F)
      );
      root.addOrReplaceChild("left_pages", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, -4.0F, -0.99F, 5.0F, 8.0F, 1.0F), PartPose.ZERO);
      root.addOrReplaceChild("right_pages", CubeListBuilder.create().texOffs(12, 10).addBox(0.0F, -4.0F, -0.01F, 5.0F, 8.0F, 1.0F), PartPose.ZERO);
      CubeListBuilder page = CubeListBuilder.create().texOffs(24, 10).addBox(0.0F, -4.0F, 0.0F, 5.0F, 8.0F, 0.005F);
      root.addOrReplaceChild("flip_page1", page, PartPose.ZERO);
      root.addOrReplaceChild("flip_page2", page, PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final BookModel.State state) {
      super.setupAnim(state);
      float openness = state.openness;
      this.leftLid.yRot = (float) Math.PI + openness;
      this.rightLid.yRot = -openness;
      this.leftPages.yRot = openness;
      this.rightPages.yRot = -openness;
      this.flipPage1.yRot = openness - openness * 2.0F * state.pageFlip1;
      this.flipPage2.yRot = openness - openness * 2.0F * state.pageFlip2;
      this.leftPages.x = Mth.sin(openness);
      this.rightPages.x = Mth.sin(openness);
      this.flipPage1.x = Mth.sin(openness);
      this.flipPage2.x = Mth.sin(openness);
   }

   public record State(float openness, float pageFlip1, float pageFlip2) {
      public static BookModel.State forAnimation(final float progress, final float pageFlip1, final float pageFlip2, final float openness) {
         return new BookModel.State((Mth.sin(progress * 0.02F) * 0.1F + 1.25F) * openness, pageFlip1, pageFlip2);
      }
   }
}
