package net.minecraft.client.gui.screens;

import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CreateBuffetWorldScreen extends Screen {
   private static final Component SEARCH_HINT = Component.translatable("createWorld.customize.buffet.search").withStyle(EditBox.SEARCH_HINT_STYLE);
   private static final int SPACING = 3;
   private static final int SEARCH_BOX_HEIGHT = 15;
   final HeaderAndFooterLayout layout;
   private final Screen parent;
   private final Consumer<Holder<Biome>> applySettings;
   final Registry<Biome> biomes;
   private CreateBuffetWorldScreen.BiomeList list;
   Holder<Biome> biome;
   private Button doneButton;

   public CreateBuffetWorldScreen(Screen p_232732_, WorldCreationContext p_232733_, Consumer<Holder<Biome>> p_232734_) {
      super(Component.translatable("createWorld.customize.buffet.title"));
      this.parent = p_232732_;
      this.applySettings = p_232734_;
      this.layout = new HeaderAndFooterLayout(this, 40, 33);
      this.biomes = p_232733_.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
      Holder<Biome> holder = this.biomes.get(Biomes.PLAINS).or(() -> this.biomes.listElements().findAny()).orElseThrow();
      this.biome = p_232733_.selectedDimensions().overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse(holder);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   protected void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(3));
      linearlayout.defaultCellSetting().alignHorizontallyCenter();
      linearlayout.addChild(new StringWidget(this.getTitle(), this.font));
      EditBox editbox = linearlayout.addChild(new EditBox(this.font, 200, 15, Component.empty()));
      CreateBuffetWorldScreen.BiomeList createbuffetworldscreen$biomelist = new CreateBuffetWorldScreen.BiomeList();
      editbox.setHint(SEARCH_HINT);
      editbox.setResponder(createbuffetworldscreen$biomelist::filterEntries);
      this.list = this.layout.addToContents(createbuffetworldscreen$biomelist);
      LinearLayout linearlayout1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = linearlayout1.addChild(Button.builder(CommonComponents.GUI_DONE, p_325363_ -> {
         this.applySettings.accept(this.biome);
         this.onClose();
      }).build());
      linearlayout1.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_325364_ -> this.onClose()).build());
      this.list.setSelected(this.list.children().stream().filter(p_232738_ -> Objects.equals(p_232738_.biome, this.biome)).findFirst().orElse(null));
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.list.updateSize(this.width, this.layout);
   }

   void updateButtonValidity() {
      this.doneButton.active = this.list.getSelected() != null;
   }

   @OnlyIn(Dist.CLIENT)
   class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
      BiomeList() {
         super(
            CreateBuffetWorldScreen.this.minecraft,
            CreateBuffetWorldScreen.this.width,
            CreateBuffetWorldScreen.this.layout.getContentHeight(),
            CreateBuffetWorldScreen.this.layout.getHeaderHeight(),
            15
         );
         this.filterEntries("");
      }

      private void filterEntries(String p_458867_) {
         Collator collator = Collator.getInstance(Locale.getDefault());
         String s = p_458867_.toLowerCase(Locale.ROOT);
         List<CreateBuffetWorldScreen.BiomeList.Entry> list = CreateBuffetWorldScreen.this.biomes
            .listElements()
            .map(p_205389_ -> new CreateBuffetWorldScreen.BiomeList.Entry((Holder.Reference<Biome>)p_205389_))
            .sorted(Comparator.comparing(p_203142_ -> p_203142_.name.getString(), collator))
            .filter(p_448014_ -> p_458867_.isEmpty() || p_448014_.name.getString().toLowerCase(Locale.ROOT).contains(s))
            .toList();
         this.replaceEntries(list);
         this.refreshScrollAmount();
      }

      public void setSelected(CreateBuffetWorldScreen.BiomeList.@Nullable Entry p_95785_) {
         super.setSelected(p_95785_);
         if (p_95785_ != null) {
            CreateBuffetWorldScreen.this.biome = p_95785_.biome;
         }

         CreateBuffetWorldScreen.this.updateButtonValidity();
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
         final Holder.Reference<Biome> biome;
         final Component name;

         public Entry(final Holder.Reference<Biome> p_205392_) {
            this.biome = p_205392_;
            Identifier identifier = p_205392_.key().identifier();
            String s = identifier.toLanguageKey("biome");
            if (Language.getInstance().has(s)) {
               this.name = Component.translatable(s);
            } else {
               this.name = Component.literal(identifier.toString());
            }
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }

         @Override
         public void renderContent(GuiGraphics p_427427_, int p_429029_, int p_429516_, boolean p_422337_, float p_426821_) {
            p_427427_.drawString(CreateBuffetWorldScreen.this.font, this.name, this.getContentX() + 5, this.getContentY() + 2, -1);
         }

         @Override
         public boolean mouseClicked(MouseButtonEvent p_430514_, boolean p_424549_) {
            BiomeList.this.setSelected(this);
            return super.mouseClicked(p_430514_, p_424549_);
         }
      }
   }
}
