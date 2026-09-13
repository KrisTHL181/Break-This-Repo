package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

public class VillagerProfessionLayer<S extends LivingEntityRenderState & VillagerDataHolderRenderState, M extends EntityModel<S> & VillagerLikeModel<S>>
   extends RenderLayer<S, M> {
   private static final Int2ObjectMap<Identifier> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap(), map -> {
      map.put(1, Identifier.withDefaultNamespace("stone"));
      map.put(2, Identifier.withDefaultNamespace("iron"));
      map.put(3, Identifier.withDefaultNamespace("gold"));
      map.put(4, Identifier.withDefaultNamespace("emerald"));
      map.put(5, Identifier.withDefaultNamespace("diamond"));
   });
   private final Object2ObjectMap<ResourceKey<VillagerType>, VillagerMetadataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<ResourceKey<VillagerProfession>, VillagerMetadataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap();
   private final ResourceManager resourceManager;
   private final String path;
   private final M noHatModel;
   private final M noHatBabyModel;

   public VillagerProfessionLayer(
      final RenderLayerParent<S, M> renderer, final ResourceManager resourceManager, final String path, final M noHatModel, final M noHatBabyModel
   ) {
      super(renderer);
      this.resourceManager = resourceManager;
      this.path = path;
      this.noHatModel = noHatModel;
      this.noHatBabyModel = noHatBabyModel;
   }

   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      if (!state.isInvisible) {
         VillagerData villagerData = state.getVillagerData();
         if (villagerData != null) {
            Holder<VillagerType> type = villagerData.type();
            Holder<VillagerProfession> profession = villagerData.profession();
            VillagerMetadataSection.Hat typeHat = this.getHatData(this.typeHatCache, "type", type);
            VillagerMetadataSection.Hat professionHat = this.getHatData(this.professionHatCache, "profession", profession);
            M model = this.getParentModel();
            Identifier typeTexture = this.getIdentifier(state.isBaby ? "baby" : "type", type);
            boolean typeHatVisible = professionHat == VillagerMetadataSection.Hat.NONE
               || professionHat == VillagerMetadataSection.Hat.PARTIAL && typeHat != VillagerMetadataSection.Hat.FULL;
            M noHatModel = state.isBaby ? this.noHatBabyModel : this.noHatModel;
            renderColoredCutoutModel(typeHatVisible ? model : noHatModel, typeTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
            if (!profession.is(VillagerProfession.NONE) && !state.isBaby) {
               Identifier professionTexture = this.getIdentifier("profession", profession);
               renderColoredCutoutModel(model, professionTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 2);
               if (!profession.is(VillagerProfession.NITWIT)) {
                  Identifier professionLevelTexture = this.getIdentifier(
                     "profession_level", (Identifier)LEVEL_LOCATIONS.get(Mth.clamp(villagerData.level(), 1, LEVEL_LOCATIONS.size()))
                  );
                  renderColoredCutoutModel(model, professionLevelTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 3);
               }
            }
         }
      }
   }

   private Identifier getIdentifier(final String type, final Identifier key) {
      return key.withPath(keyPath -> "textures/entity/" + this.path + "/" + type + "/" + keyPath + ".png");
   }

   private Identifier getIdentifier(final String type, final Holder<?> holder) {
      return holder.unwrapKey().map(k -> this.getIdentifier(type, k.identifier())).orElse(MissingTextureAtlasSprite.getLocation());
   }

   public <K> VillagerMetadataSection.Hat getHatData(
      final Object2ObjectMap<ResourceKey<K>, VillagerMetadataSection.Hat> cache, final String name, final Holder<K> holder
   ) {
      ResourceKey<K> key = holder.unwrapKey().orElse(null);
      return key == null
         ? VillagerMetadataSection.Hat.NONE
         : (VillagerMetadataSection.Hat)cache.computeIfAbsent(
            key, k -> this.resourceManager.getResource(this.getIdentifier(name, key.identifier())).flatMap(resource -> {
               try {
                  return resource.metadata().getSection(VillagerMetadataSection.TYPE).map(VillagerMetadataSection::hat);
               } catch (IOException ignored) {
                  return Optional.empty();
               }
            }).orElse(VillagerMetadataSection.Hat.NONE)
         );
   }
}
