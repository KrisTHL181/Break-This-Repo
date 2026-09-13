package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.jspecify.annotations.Nullable;

public class EquipmentLayerRenderer {
   private static final int NO_LAYER_COLOR = 0;
   private final EquipmentAssetManager equipmentAssets;
   private final Function<EquipmentLayerRenderer.LayerTextureKey, Identifier> layerTextureLookup;
   private final Function<EquipmentLayerRenderer.TrimTextureKey, PalettedTextureManager.Handle> trimTextureLookup;

   public EquipmentLayerRenderer(final EquipmentAssetManager equipmentAssets, final PalettedTextureManager palettedTextures) {
      this.equipmentAssets = equipmentAssets;
      this.layerTextureLookup = Util.memoize(key -> key.layer.getTextureLocation(key.layerType));
      this.trimTextureLookup = Util.memoize(key -> palettedTextures.getOrPrepare(key.baseTexture(), key.paletteId()));
   }

   public <S> void renderLayers(
      final EquipmentClientInfo.LayerType layerType,
      final ResourceKey<EquipmentAsset> equipmentAssetId,
      final Model<? super S> model,
      final S state,
      final ItemStack itemStack,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final int outlineColor
   ) {
      this.renderLayers(layerType, equipmentAssetId, model, state, itemStack, poseStack, submitNodeCollector, lightCoords, null, outlineColor, 1);
   }

   public <S> void renderLayers(
      final EquipmentClientInfo.LayerType layerType,
      final ResourceKey<EquipmentAsset> equipmentAssetId,
      final Model<? super S> model,
      final S state,
      final ItemStack itemStack,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final @Nullable Identifier playerTextureOverride,
      final int outlineColor,
      final int order
   ) {
      EquipmentClientInfo equipmentInfo = this.equipmentAssets.get(equipmentAssetId);
      List<EquipmentClientInfo.Layer> layers = equipmentInfo.getLayers(layerType);
      if (!layers.isEmpty()) {
         int dyeColor = DyedItemColor.getOrDefault(itemStack, 0);
         boolean hasFoil = itemStack.hasFoil();
         ArmorTrim trim = itemStack.get(DataComponents.TRIM);
         boolean hasTrim = trim != null && layerType != EquipmentClientInfo.LayerType.HUMANOID_BABY;
         boolean renderShaderGlint = hasFoil && !hasTrim;
         int nextOrder = order;

         for (EquipmentClientInfo.Layer layer : layers) {
            int color = getColorForLayer(layer, dyeColor);
            if (color != 0) {
               Identifier layerTexture = layer.usePlayerTexture() && playerTextureOverride != null
                  ? playerTextureOverride
                  : this.layerTextureLookup.apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layer));
               RenderType renderType = renderShaderGlint ? RenderTypes.armorCutoutNoCullGlint(layerTexture) : RenderTypes.armorCutoutNoCull(layerTexture);
               submitNodeCollector.order(nextOrder++)
                  .submitModel(model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, color, null, outlineColor);
               renderShaderGlint = false;
            }
         }

         if (hasTrim) {
            PalettedTextureManager.Handle textureHandle = this.trimTextureLookup
               .apply(new EquipmentLayerRenderer.TrimTextureKey(trim, layerType, equipmentInfo));
            RenderType renderType = RenderTypes.armorTrim(textureHandle.textureLocation(), trim.pattern().value().decal());
            submitNodeCollector.order(nextOrder++)
               .submitModel(model, state, poseStack, renderType, lightCoords, OverlayTexture.NO_OVERLAY, -1, textureHandle, outlineColor);
            if (hasFoil) {
               submitNodeCollector.order(nextOrder++)
                  .submitModel(model, state, poseStack, RenderTypes.trimmedArmorGlint(), lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0);
            }
         }
      }
   }

   private static int getColorForLayer(final EquipmentClientInfo.Layer layer, final int dyeColor) {
      Optional<EquipmentClientInfo.Dyeable> dyeable = layer.dyeable();
      if (dyeable.isPresent()) {
         int colorWhenUndyed = dyeable.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
         return dyeColor != 0 ? dyeColor : colorWhenUndyed;
      } else {
         return -1;
      }
   }

   private record LayerTextureKey(EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer) {
   }

   private record TrimTextureKey(ArmorTrim trim, EquipmentClientInfo.LayerType layerType, EquipmentClientInfo equipmentInfo) {
      public Identifier baseTexture() {
         return this.trim.pattern().value().assetId().withPath(path -> this.layerType.trimAssetPrefix() + "/" + path);
      }

      public Identifier paletteId() {
         Identifier paletteId = this.trim.material().value().paletteId();
         return this.equipmentInfo.trimPaletteReplacements().getOrDefault(paletteId, paletteId);
      }
   }
}
