package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MissingItemModel implements ItemModel {
   private final List<BakedQuad> quads;
   private final Supplier<Vector3fc[]> extents;
   private final ModelRenderProperties properties;

   public MissingItemModel(List<BakedQuad> p_392883_, ModelRenderProperties p_391347_) {
      this.quads = p_392883_;
      this.properties = p_391347_;
      this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(this.quads));
   }

   @Override
   public void update(
      ItemStackRenderState p_378769_,
      ItemStack p_377587_,
      ItemModelResolver p_375595_,
      ItemDisplayContext p_376141_,
      @Nullable ClientLevel p_378330_,
      @Nullable ItemOwner p_426387_,
      int p_377588_
   ) {
      p_378769_.appendModelIdentityElement(this);
      ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_378769_.newLayer();
      itemstackrenderstate$layerrenderstate.setRenderType(Sheets.cutoutBlockSheet());
      this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_376141_);
      itemstackrenderstate$layerrenderstate.setExtents(this.extents);
      itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
   }
}
