package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsRegion;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RegionSelectionPreference;
import com.mojang.realmsclient.dto.RegionSelectionPreferenceDto;
import com.mojang.realmsclient.dto.ServiceQuality;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsSettingsTab extends GridLayoutTab implements RealmsConfigurationTab {
   private static final int COMPONENT_WIDTH = 212;
   private static final int EXTRA_SPACING = 2;
   private static final int DEFAULT_SPACING = 6;
   static final Component TITLE = Component.translatable("mco.configure.world.settings.title");
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private static final Component REGION_PREFERENCE_LABEL = Component.translatable("mco.configure.world.region_preference");
   private static final Tooltip REALM_NAME_VALIDATION_ERROR_TOOLTIP = Tooltip.create(Component.translatable("mco.configure.world.name.validation.whitespace"));
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private RealmsServer serverData;
   private final Map<RealmsRegion, ServiceQuality> regionServiceQuality;
   final Button closeOpenButton;
   private final EditBox descEdit;
   private final EditBox nameEdit;
   private final StringWidget selectedRegionStringWidget;
   private final ImageWidget selectedRegionImageWidget;
   private RealmsSettingsTab.RegionSelection preferredRegionSelection;

   RealmsSettingsTab(RealmsConfigureWorldScreen p_409860_, Minecraft p_408716_, RealmsServer p_407031_, Map<RealmsRegion, ServiceQuality> p_410637_) {
      super(TITLE);
      this.configurationScreen = p_409860_;
      this.minecraft = p_408716_;
      this.serverData = p_407031_;
      this.regionServiceQuality = p_410637_;
      GridLayout.RowHelper gridlayout$rowhelper = this.layout.rowSpacing(6).createRowHelper(1);
      gridlayout$rowhelper.addChild(new StringWidget(NAME_LABEL, p_409860_.getFont()));
      this.nameEdit = new EditBox(p_408716_.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.name"));
      this.nameEdit.setMaxLength(32);
      this.nameEdit.setResponder(p_447761_ -> {
         if (!this.isRealmNameValid()) {
            this.nameEdit.setTextColor(-2142128);
            this.nameEdit.setTooltip(REALM_NAME_VALIDATION_ERROR_TOOLTIP);
         } else {
            this.nameEdit.setTooltip(null);
            this.nameEdit.setTextColor(-2039584);
         }
      });
      gridlayout$rowhelper.addChild(this.nameEdit);
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      gridlayout$rowhelper.addChild(new StringWidget(DESCRIPTION_LABEL, p_409860_.getFont()));
      this.descEdit = new EditBox(p_408716_.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.description"));
      this.descEdit.setMaxLength(32);
      gridlayout$rowhelper.addChild(this.descEdit);
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      gridlayout$rowhelper.addChild(new StringWidget(REGION_PREFERENCE_LABEL, p_409860_.getFont()));
      EqualSpacingLayout equalspacinglayout = new EqualSpacingLayout(0, 0, 212, 9, EqualSpacingLayout.Orientation.HORIZONTAL);
      this.selectedRegionStringWidget = equalspacinglayout.addChild(new StringWidget(192, 9, Component.empty(), p_409860_.getFont()));
      this.selectedRegionImageWidget = equalspacinglayout.addChild(ImageWidget.sprite(10, 8, ServiceQuality.UNKNOWN.getIcon()));
      gridlayout$rowhelper.addChild(equalspacinglayout);
      gridlayout$rowhelper.addChild(
         Button.builder(Component.translatable("mco.configure.world.buttons.region_preference"), p_408104_ -> this.openPreferenceSelector())
            .bounds(0, 0, 212, 20)
            .build()
      );
      gridlayout$rowhelper.addChild(SpacerElement.height(2));
      this.closeOpenButton = gridlayout$rowhelper.addChild(
         Button.builder(
               Component.empty(),
               p_410205_ -> {
                  if (p_407031_.state == RealmsServer.State.OPEN) {
                     p_408716_.setScreen(
                        RealmsPopups.customPopupScreen(
                           p_409860_,
                           Component.translatable("mco.configure.world.close.question.title"),
                           Component.translatable("mco.configure.world.close.question.line1"),
                           p_408045_ -> {
                              this.save();
                              p_409860_.closeTheWorld();
                           }
                        )
                     );
                  } else {
                     this.save();
                     p_409860_.openTheWorld(false);
                  }
               }
            )
            .bounds(0, 0, 212, 20)
            .build()
      );
      this.closeOpenButton.active = false;
      this.updateData(p_407031_);
   }

   private static MutableComponent getTranslatableFromPreference(RealmsSettingsTab.RegionSelection p_406363_) {
      return (p_406363_.preference().equals(RegionSelectionPreference.MANUAL) && p_406363_.region() != null
            ? Component.translatable(p_406363_.region().translationKey)
            : Component.translatable(p_406363_.preference().translationKey))
         .withStyle(ChatFormatting.GRAY);
   }

   private static Identifier getServiceQualityIcon(RealmsSettingsTab.RegionSelection p_407300_, Map<RealmsRegion, ServiceQuality> p_406645_) {
      if (p_407300_.region() != null && p_406645_.containsKey(p_407300_.region())) {
         ServiceQuality servicequality = p_406645_.getOrDefault(p_407300_.region(), ServiceQuality.UNKNOWN);
         return servicequality.getIcon();
      } else {
         return ServiceQuality.UNKNOWN.getIcon();
      }
   }

   private boolean isRealmNameValid() {
      String s = this.nameEdit.getValue();
      String s1 = s.trim();
      return !s1.isEmpty() && s.length() == s1.length();
   }

   private void openPreferenceSelector() {
      this.minecraft
         .setScreen(
            new RealmsPreferredRegionSelectionScreen(
               this.configurationScreen, this::applyRegionPreferenceSelection, this.regionServiceQuality, this.preferredRegionSelection
            )
         );
   }

   private void applyRegionPreferenceSelection(RegionSelectionPreference p_407377_, RealmsRegion p_410575_) {
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(p_407377_, p_410575_);
      this.updateRegionPreferenceValues();
   }

   private void updateRegionPreferenceValues() {
      this.selectedRegionStringWidget.setMessage(getTranslatableFromPreference(this.preferredRegionSelection));
      this.selectedRegionImageWidget.updateResource(getServiceQualityIcon(this.preferredRegionSelection, this.regionServiceQuality));
      this.selectedRegionImageWidget.visible = this.preferredRegionSelection.preference == RegionSelectionPreference.MANUAL;
   }

   @Override
   public void onSelected(RealmsServer p_408872_) {
      this.updateData(p_408872_);
   }

   @Override
   public void updateData(RealmsServer p_407369_) {
      this.serverData = p_407369_;
      if (p_407369_.regionSelectionPreference == null) {
         p_407369_.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
      }

      if (p_407369_.regionSelectionPreference.regionSelectionPreference == RegionSelectionPreference.MANUAL
         && p_407369_.regionSelectionPreference.preferredRegion == null) {
         Optional<RealmsRegion> optional = this.regionServiceQuality.keySet().stream().findFirst();
         optional.ifPresent(p_406696_ -> p_407369_.regionSelectionPreference.preferredRegion = p_406696_);
      }

      String s = p_407369_.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close" : "mco.configure.world.buttons.open";
      this.closeOpenButton.setMessage(Component.translatable(s));
      this.closeOpenButton.active = true;
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(
         p_407369_.regionSelectionPreference.regionSelectionPreference, p_407369_.regionSelectionPreference.preferredRegion
      );
      this.nameEdit.setValue(Objects.requireNonNullElse(p_407369_.getName(), ""));
      this.descEdit.setValue(p_407369_.getDescription());
      this.updateRegionPreferenceValues();
   }

   @Override
   public void onDeselected(RealmsServer p_406052_) {
      this.save();
   }

   public void save() {
      String s = this.nameEdit.getValue().trim();
      if (this.serverData.regionSelectionPreference == null
         || !Objects.equals(s, this.serverData.name)
         || !Objects.equals(this.descEdit.getValue(), this.serverData.motd)
         || this.preferredRegionSelection.preference() != this.serverData.regionSelectionPreference.regionSelectionPreference
         || this.preferredRegionSelection.region() != this.serverData.regionSelectionPreference.preferredRegion) {
         this.configurationScreen.saveSettings(s, this.descEdit.getValue(), this.preferredRegionSelection.preference(), this.preferredRegionSelection.region());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record RegionSelection(RegionSelectionPreference preference, @Nullable RealmsRegion region) {
   }
}
