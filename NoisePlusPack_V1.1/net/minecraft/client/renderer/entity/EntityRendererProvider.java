package net.minecraft.client.renderer.entity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface EntityRendererProvider<T extends Entity> {
   EntityRenderer<T, ?> create(EntityRendererProvider.Context var1);

   @OnlyIn(Dist.CLIENT)
   class Context {
      private final EntityRenderDispatcher entityRenderDispatcher;
      private final ItemModelResolver itemModelResolver;
      private final MapRenderer mapRenderer;
      private final BlockRenderDispatcher blockRenderDispatcher;
      private final ResourceManager resourceManager;
      private final EntityModelSet modelSet;
      private final EquipmentAssetManager equipmentAssets;
      private final Font font;
      private final EquipmentLayerRenderer equipmentRenderer;
      private final AtlasManager atlasManager;
      private final PlayerSkinRenderCache playerSkinRenderCache;

      public Context(
         EntityRenderDispatcher p_234590_,
         ItemModelResolver p_376231_,
         MapRenderer p_361143_,
         BlockRenderDispatcher p_234592_,
         ResourceManager p_234594_,
         EntityModelSet p_234595_,
         EquipmentAssetManager p_377420_,
         AtlasManager p_431176_,
         Font p_234596_,
         PlayerSkinRenderCache p_431507_
      ) {
         this.entityRenderDispatcher = p_234590_;
         this.itemModelResolver = p_376231_;
         this.mapRenderer = p_361143_;
         this.blockRenderDispatcher = p_234592_;
         this.resourceManager = p_234594_;
         this.modelSet = p_234595_;
         this.equipmentAssets = p_377420_;
         this.font = p_234596_;
         this.atlasManager = p_431176_;
         this.playerSkinRenderCache = p_431507_;
         this.equipmentRenderer = new EquipmentLayerRenderer(p_377420_, p_431176_.getAtlasOrThrow(AtlasIds.ARMOR_TRIMS));
      }

      public EntityRenderDispatcher getEntityRenderDispatcher() {
         return this.entityRenderDispatcher;
      }

      public ItemModelResolver getItemModelResolver() {
         return this.itemModelResolver;
      }

      public MapRenderer getMapRenderer() {
         return this.mapRenderer;
      }

      public BlockRenderDispatcher getBlockRenderDispatcher() {
         return this.blockRenderDispatcher;
      }

      public ResourceManager getResourceManager() {
         return this.resourceManager;
      }

      public EntityModelSet getModelSet() {
         return this.modelSet;
      }

      public EquipmentAssetManager getEquipmentAssets() {
         return this.equipmentAssets;
      }

      public EquipmentLayerRenderer getEquipmentRenderer() {
         return this.equipmentRenderer;
      }

      public MaterialSet getMaterials() {
         return this.atlasManager;
      }

      public TextureAtlas getAtlas(Identifier p_456157_) {
         return this.atlasManager.getAtlasOrThrow(p_456157_);
      }

      public ModelPart bakeLayer(ModelLayerLocation p_174024_) {
         return this.modelSet.bakeLayer(p_174024_);
      }

      public Font getFont() {
         return this.font;
      }

      public PlayerSkinRenderCache getPlayerSkinRenderCache() {
         return this.playerSkinRenderCache;
      }
   }
}
