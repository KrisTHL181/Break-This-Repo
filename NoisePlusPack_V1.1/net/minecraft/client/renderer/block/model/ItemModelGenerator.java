package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerator implements UnbakedModel {
   public static final Identifier GENERATED_ITEM_MODEL_ID = Identifier.withDefaultNamespace("builtin/generated");
   public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
   private static final float MIN_Z = 7.5F;
   private static final float MAX_Z = 8.5F;
   private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();
   private static final BlockElementFace.UVs SOUTH_FACE_UVS = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
   private static final BlockElementFace.UVs NORTH_FACE_UVS = new BlockElementFace.UVs(16.0F, 0.0F, 0.0F, 16.0F);
   private static final float UV_SHRINK = 0.1F;

   @Override
   public TextureSlots.Data textureSlots() {
      return TEXTURE_SLOTS;
   }

   @Override
   public UnbakedGeometry geometry() {
      return ItemModelGenerator::bake;
   }

   @Override
   public UnbakedModel.@Nullable GuiLight guiLight() {
      return UnbakedModel.GuiLight.FRONT;
   }

   private static QuadCollection bake(TextureSlots p_377946_, ModelBaker p_392334_, ModelState p_375548_, ModelDebugName p_397121_) {
      List<BlockElement> list = new ArrayList<>();

      for (int i = 0; i < LAYERS.size(); i++) {
         String s = LAYERS.get(i);
         Material material = p_377946_.getMaterial(s);
         if (material == null) {
            break;
         }

         SpriteContents spritecontents = p_392334_.sprites().get(material, p_397121_).contents();
         list.addAll(processFrames(i, s, spritecontents));
      }

      return SimpleUnbakedGeometry.bake(list, p_377946_, p_392334_, p_375548_, p_397121_);
   }

   private static List<BlockElement> processFrames(int p_111639_, String p_111640_, SpriteContents p_251768_) {
      Map<Direction, BlockElementFace> map = Map.of(
         Direction.SOUTH,
         new BlockElementFace(null, p_111639_, p_111640_, SOUTH_FACE_UVS, Quadrant.R0),
         Direction.NORTH,
         new BlockElementFace(null, p_111639_, p_111640_, NORTH_FACE_UVS, Quadrant.R0)
      );
      List<BlockElement> list = new ArrayList<>();
      list.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
      list.addAll(createSideElements(p_251768_, p_111640_, p_111639_));
      return list;
   }

   private static List<BlockElement> createSideElements(SpriteContents p_248810_, String p_111663_, int p_111664_) {
      float f = 16.0F / p_248810_.width();
      float f1 = 16.0F / p_248810_.height();
      List<BlockElement> list = new ArrayList<>();

      for (ItemModelGenerator.SideFace itemmodelgenerator$sideface : getSideFaces(p_248810_)) {
         float f2 = itemmodelgenerator$sideface.x();
         float f3 = itemmodelgenerator$sideface.y();
         ItemModelGenerator.SideDirection itemmodelgenerator$sidedirection = itemmodelgenerator$sideface.facing();
         float f4 = f2 + 0.1F;
         float f5 = f2 + 1.0F - 0.1F;
         float f6;
         float f7;
         if (itemmodelgenerator$sidedirection.isHorizontal()) {
            f6 = f3 + 0.1F;
            f7 = f3 + 1.0F - 0.1F;
         } else {
            f6 = f3 + 1.0F - 0.1F;
            f7 = f3 + 0.1F;
         }

         float f8 = f2;
         float f9 = f3;
         float f10 = f2;
         float f11 = f3;
         switch (itemmodelgenerator$sidedirection) {
            case UP:
               f10 = f2 + 1.0F;
               break;
            case DOWN:
               f10 = f2 + 1.0F;
               f9 = f3 + 1.0F;
               f11 = f3 + 1.0F;
               break;
            case LEFT:
               f11 = f3 + 1.0F;
               break;
            case RIGHT:
               f8 = f2 + 1.0F;
               f10 = f2 + 1.0F;
               f11 = f3 + 1.0F;
         }

         f8 *= f;
         f10 *= f;
         f9 *= f1;
         f11 *= f1;
         f9 = 16.0F - f9;
         f11 = 16.0F - f11;
         Map<Direction, BlockElementFace> map = Map.of(
            itemmodelgenerator$sidedirection.getDirection(),
            new BlockElementFace(null, p_111664_, p_111663_, new BlockElementFace.UVs(f4 * f, f6 * f, f5 * f1, f7 * f1), Quadrant.R0)
         );
         switch (itemmodelgenerator$sidedirection) {
            case UP:
               list.add(new BlockElement(new Vector3f(f8, f9, 7.5F), new Vector3f(f10, f9, 8.5F), map));
               break;
            case DOWN:
               list.add(new BlockElement(new Vector3f(f8, f11, 7.5F), new Vector3f(f10, f11, 8.5F), map));
               break;
            case LEFT:
               list.add(new BlockElement(new Vector3f(f8, f9, 7.5F), new Vector3f(f8, f11, 8.5F), map));
               break;
            case RIGHT:
               list.add(new BlockElement(new Vector3f(f10, f9, 7.5F), new Vector3f(f10, f11, 8.5F), map));
         }
      }

      return list;
   }

   private static Collection<ItemModelGenerator.SideFace> getSideFaces(SpriteContents p_457701_) {
      int i = p_457701_.width();
      int j = p_457701_.height();
      Set<ItemModelGenerator.SideFace> set = new HashSet<>();
      p_457701_.getUniqueFrames().forEach(p_389479_ -> {
         for (int k = 0; k < j; k++) {
            for (int l = 0; l < i; l++) {
               boolean flag = !isTransparent(p_457701_, p_389479_, l, k, i, j);
               if (flag) {
                  checkTransition(ItemModelGenerator.SideDirection.UP, set, p_457701_, p_389479_, l, k, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.DOWN, set, p_457701_, p_389479_, l, k, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.LEFT, set, p_457701_, p_389479_, l, k, i, j);
                  checkTransition(ItemModelGenerator.SideDirection.RIGHT, set, p_457701_, p_389479_, l, k, i, j);
               }
            }
         }
      });
      return set;
   }

   private static void checkTransition(
      ItemModelGenerator.SideDirection p_453976_,
      Set<ItemModelGenerator.SideFace> p_458979_,
      SpriteContents p_249847_,
      int p_250616_,
      int p_251416_,
      int p_249664_,
      int p_250174_,
      int p_250897_
   ) {
      if (isTransparent(p_249847_, p_250616_, p_251416_ - p_453976_.direction.getStepX(), p_249664_ - p_453976_.direction.getStepY(), p_250174_, p_250897_)) {
         p_458979_.add(new ItemModelGenerator.SideFace(p_453976_, p_251416_, p_249664_));
      }
   }

   private static boolean isTransparent(SpriteContents p_249650_, int p_250692_, int p_251914_, int p_252343_, int p_250258_, int p_248997_) {
      return p_251914_ >= 0 && p_252343_ >= 0 && p_251914_ < p_250258_ && p_252343_ < p_248997_
         ? p_249650_.isTransparent(p_250692_, p_251914_, p_252343_)
         : true;
   }

   @OnlyIn(Dist.CLIENT)
   enum SideDirection {
      UP(Direction.UP),
      DOWN(Direction.DOWN),
      LEFT(Direction.EAST),
      RIGHT(Direction.WEST);

      final Direction direction;

      SideDirection(final Direction p_455854_) {
         this.direction = p_455854_;
      }

      public Direction getDirection() {
         return this.direction;
      }

      boolean isHorizontal() {
         return this == DOWN || this == UP;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SideFace(ItemModelGenerator.SideDirection facing, int x, int y) {
   }
}
