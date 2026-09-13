package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PresetFlatWorldScreen extends Screen {
   static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
   public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
   private final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen p_96379_) {
      super(Component.translatable("createWorld.customize.presets.title"));
      this.parent = p_96379_;
   }

   private static @Nullable FlatLayerInfo getLayerInfoFromString(HolderGetter<Block> p_259695_, String p_259185_, int p_259723_) {
      List<String> list = Splitter.on('*').limit(2).splitToList(p_259185_);
      int i;
      String s;
      if (list.size() == 2) {
         s = list.get(1);

         try {
            i = Math.max(Integer.parseInt(list.get(0)), 0);
         } catch (NumberFormatException numberformatexception) {
            LOGGER.error("Error while parsing flat world string", numberformatexception);
            return null;
         }
      } else {
         s = list.get(0);
         i = 1;
      }

      int j = Math.min(p_259723_ + i, DimensionType.Y_SIZE);
      int k = j - p_259723_;

      Optional<Holder.Reference<Block>> optional;
      try {
         optional = p_259695_.get(ResourceKey.create(Registries.BLOCK, Identifier.parse(s)));
      } catch (Exception exception) {
         LOGGER.error("Error while parsing flat world string", exception);
         return null;
      }

      if (optional.isEmpty()) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", s);
         return null;
      } else {
         return new FlatLayerInfo(k, optional.get().value());
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(HolderGetter<Block> p_259080_, String p_260301_) {
      List<FlatLayerInfo> list = Lists.newArrayList();
      String[] astring = p_260301_.split(",");
      int i = 0;

      for (String s : astring) {
         FlatLayerInfo flatlayerinfo = getLayerInfoFromString(p_259080_, s, i);
         if (flatlayerinfo == null) {
            return Collections.emptyList();
         }

         int j = DimensionType.Y_SIZE - i;
         if (j > 0) {
            list.add(flatlayerinfo.heightLimited(j));
            i += flatlayerinfo.getHeight();
         }
      }

      return list;
   }

   public static FlatLevelGeneratorSettings fromString(
      HolderGetter<Block> p_259084_,
      HolderGetter<Biome> p_259583_,
      HolderGetter<StructureSet> p_259610_,
      HolderGetter<PlacedFeature> p_259243_,
      String p_259508_,
      FlatLevelGeneratorSettings p_259417_
   ) {
      Iterator<String> iterator = Splitter.on(';').split(p_259508_).iterator();
      if (!iterator.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(p_259583_, p_259610_, p_259243_);
      }

      List<FlatLayerInfo> list = getLayersInfoFromString(p_259084_, iterator.next());
      if (list.isEmpty()) {
         return FlatLevelGeneratorSettings.getDefault(p_259583_, p_259610_, p_259243_);
      }

      Holder.Reference<Biome> reference = p_259583_.getOrThrow(DEFAULT_BIOME);
      Holder<Biome> holder = reference;
      if (iterator.hasNext()) {
         String s = iterator.next();
         holder = Optional.ofNullable(Identifier.tryParse(s))
            .map(p_448021_ -> ResourceKey.create(Registries.BIOME, p_448021_))
            .flatMap(p_259583_::get)
            .orElseGet(() -> {
               LOGGER.warn("Invalid biome: {}", s);
               return reference;
            });
      }

      return p_259417_.withBiomeAndLayers(list, p_259417_.structureOverrides(), holder);
   }

   static String save(FlatLevelGeneratorSettings p_205394_) {
      StringBuilder stringbuilder = new StringBuilder();

      for (int i = 0; i < p_205394_.getLayersInfo().size(); i++) {
         if (i > 0) {
            stringbuilder.append(",");
         }

         stringbuilder.append(p_205394_.getLayersInfo().get(i));
      }

      stringbuilder.append(";");
      stringbuilder.append(p_205394_.getBiome().unwrapKey().map(ResourceKey::identifier).orElseThrow(() -> new IllegalStateException("Biome not registered")));
      return stringbuilder.toString();
   }

   @Override
   protected void init() {
      this.shareText = Component.translatable("createWorld.customize.presets.share");
      this.listText = Component.translatable("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      WorldCreationContext worldcreationcontext = this.parent.parent.getUiState().getSettings();
      RegistryAccess registryaccess = worldcreationcontext.worldgenLoadContext();
      FeatureFlagSet featureflagset = worldcreationcontext.dataConfiguration().enabledFeatures();
      HolderGetter<Biome> holdergetter = registryaccess.lookupOrThrow(Registries.BIOME);
      HolderGetter<StructureSet> holdergetter1 = registryaccess.lookupOrThrow(Registries.STRUCTURE_SET);
      HolderGetter<PlacedFeature> holdergetter2 = registryaccess.lookupOrThrow(Registries.PLACED_FEATURE);
      HolderGetter<Block> holdergetter3 = registryaccess.lookupOrThrow(Registries.BLOCK).filterFeatures(featureflagset);
      this.export.setValue(save(this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = this.addRenderableWidget(new PresetFlatWorldScreen.PresetsList(registryaccess, featureflagset));
      this.selectButton = this.addRenderableWidget(
         Button.builder(
               Component.translatable("createWorld.customize.presets.select"),
               p_280822_ -> {
                  FlatLevelGeneratorSettings flatlevelgeneratorsettings = fromString(
                     holdergetter3, holdergetter, holdergetter1, holdergetter2, this.export.getValue(), this.settings
                  );
                  this.parent.setConfig(flatlevelgeneratorsettings);
                  this.minecraft.setScreen(this.parent);
               }
            )
            .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_280823_ -> this.minecraft.setScreen(this.parent))
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build()
      );
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   @Override
   public boolean mouseScrolled(double p_96381_, double p_96382_, double p_96383_, double p_297555_) {
      return this.list.mouseScrolled(p_96381_, p_96382_, p_96383_, p_297555_);
   }

   @Override
   public void resize(int p_96391_, int p_96392_) {
      String s = this.export.getValue();
      this.init(p_96391_, p_96392_);
      this.export.setValue(s);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void render(GuiGraphics p_282713_, int p_281914_, int p_283700_, float p_283598_) {
      super.render(p_282713_, p_281914_, p_283700_, p_283598_);
      p_282713_.drawCenteredString(this.font, this.title, this.width / 2, 8, -1);
      p_282713_.drawString(this.font, this.shareText, 51, 30, -6250336);
      p_282713_.drawString(this.font, this.listText, 51, 68, -6250336);
      this.export.render(p_282713_, p_281914_, p_283700_, p_283598_);
   }

   public void updateButtonValidity(boolean p_96450_) {
      this.selectButton.active = p_96450_ || this.export.getValue().length() > 1;
   }

   @OnlyIn(Dist.CLIENT)
   class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList(final RegistryAccess p_259278_, final FeatureFlagSet p_259076_) {
         super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height - 117, 80, 24);

         for (Holder<FlatLevelGeneratorPreset> holder : p_259278_.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
            .getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
            Set<Block> set = holder.value()
               .settings()
               .getLayersInfo()
               .stream()
               .map(p_259579_ -> p_259579_.getBlockState().getBlock())
               .filter(p_259421_ -> !p_259421_.isEnabled(p_259076_))
               .collect(Collectors.toSet());
            if (!set.isEmpty()) {
               PresetFlatWorldScreen.LOGGER
                  .info(
                     "Discarding flat world preset {} since it contains experimental blocks {}",
                     holder.unwrapKey().map(p_448022_ -> p_448022_.identifier().toString()).orElse("<unknown>"),
                     set
                  );
            } else {
               this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(holder));
            }
         }
      }

      public void setSelected(PresetFlatWorldScreen.PresetsList.@Nullable Entry p_96472_) {
         super.setSelected(p_96472_);
         PresetFlatWorldScreen.this.updateButtonValidity(p_96472_ != null);
      }

      @Override
      public boolean keyPressed(KeyEvent p_426196_) {
         if (super.keyPressed(p_426196_)) {
            return true;
         }

         if (p_426196_.isSelection() && this.getSelected() != null) {
            this.getSelected().select();
         }

         return false;
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private static final Identifier STATS_ICON_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/stats_icons.png");
         private final FlatLevelGeneratorPreset preset;
         private final Component name;

         public Entry(final Holder<FlatLevelGeneratorPreset> p_232758_) {
            this.preset = p_232758_.value();
            this.name = p_232758_.unwrapKey()
               .map(p_448023_ -> Component.translatable(p_448023_.identifier().toLanguageKey("flat_world_preset")))
               .orElse(PresetFlatWorldScreen.UNKNOWN_PRESET);
         }

         @Override
         public void renderContent(GuiGraphics p_425415_, int p_429399_, int p_429491_, boolean p_424635_, float p_427961_) {
            this.blitSlot(p_425415_, this.getContentX(), this.getContentY(), this.preset.displayItem().value());
            p_425415_.drawString(PresetFlatWorldScreen.this.font, this.name, this.getContentX() + 18 + 5, this.getContentY() + 6, -1);
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent p_427307_, boolean p_430283_) {
            this.select();
            return super.mouseClicked(p_427307_, p_430283_);
         }

         void select() {
            PresetsList.this.setSelected(this);
            PresetFlatWorldScreen.this.settings = this.preset.settings();
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart(false);
         }

         private void blitSlot(GuiGraphics p_283196_, int p_282036_, int p_281683_, Item p_282242_) {
            this.blitSlotBg(p_283196_, p_282036_ + 1, p_281683_ + 1);
            p_283196_.renderFakeItem(new ItemStack(p_282242_), p_282036_ + 2, p_281683_ + 2);
         }

         private void blitSlotBg(GuiGraphics p_281359_, int p_282978_, int p_283152_) {
            p_281359_.blitSprite(RenderPipelines.GUI_TEXTURED, PresetFlatWorldScreen.SLOT_SPRITE, p_282978_, p_283152_, 18, 18);
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }
      }
   }
}
