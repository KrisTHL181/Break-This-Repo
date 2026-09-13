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
import org.jspecify.annotations.Nullable;

public class RealmsSettingsTab extends GridLayoutTab implements RealmsConfigurationTab {
   private static final int COMPONENT_WIDTH = 212;
   private static final int EXTRA_SPACING = 2;
   private static final int DEFAULT_SPACING = 6;
   public static final Component TITLE = Component.translatable("mco.configure.world.settings.title");
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private static final Component REGION_PREFERENCE_LABEL = Component.translatable("mco.configure.world.region_preference");
   private static final Tooltip REALM_NAME_VALIDATION_ERROR_TOOLTIP = Tooltip.create(Component.translatable("mco.configure.world.name.validation.whitespace"));
   private final RealmsConfigureWorldScreen configurationScreen;
   private final Minecraft minecraft;
   private RealmsServer serverData;
   private final Map<RealmsRegion, ServiceQuality> regionServiceQuality;
   private final Button closeOpenButton;
   private final EditBox descEdit;
   private final EditBox nameEdit;
   private final StringWidget selectedRegionStringWidget;
   private final ImageWidget selectedRegionImageWidget;
   private RealmsSettingsTab.RegionSelection preferredRegionSelection;

   public RealmsSettingsTab(
      final RealmsConfigureWorldScreen configurationScreen,
      final Minecraft minecraft,
      final RealmsServer serverData,
      final Map<RealmsRegion, ServiceQuality> regionServiceQuality
   ) {
      super(TITLE);
      this.configurationScreen = configurationScreen;
      this.minecraft = minecraft;
      this.serverData = serverData;
      this.regionServiceQuality = regionServiceQuality;
      GridLayout.RowHelper helper = this.layout.rowSpacing(6).createRowHelper(1);
      helper.addChild(new StringWidget(NAME_LABEL, configurationScreen.getFont()));
      this.nameEdit = new EditBox(minecraft.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.name"));
      this.nameEdit.setMaxLength(32);
      this.nameEdit.setResponder(value -> {
         if (!this.isRealmNameValid()) {
            this.nameEdit.setTextColor(-2142128);
            this.nameEdit.setTooltip(REALM_NAME_VALIDATION_ERROR_TOOLTIP);
         } else {
            this.nameEdit.setTooltip(null);
            this.nameEdit.setTextColor(-2039584);
         }
      });
      helper.addChild(this.nameEdit);
      helper.addChild(SpacerElement.height(2));
      helper.addChild(new StringWidget(DESCRIPTION_LABEL, configurationScreen.getFont()));
      this.descEdit = new EditBox(minecraft.font, 0, 0, 212, 20, Component.translatable("mco.configure.world.description"));
      this.descEdit.setMaxLength(32);
      helper.addChild(this.descEdit);
      helper.addChild(SpacerElement.height(2));
      helper.addChild(new StringWidget(REGION_PREFERENCE_LABEL, configurationScreen.getFont()));
      EqualSpacingLayout selectedRegion = new EqualSpacingLayout(0, 0, 212, 9, EqualSpacingLayout.Orientation.HORIZONTAL);
      this.selectedRegionStringWidget = selectedRegion.addChild(new StringWidget(192, 9, Component.empty(), configurationScreen.getFont()));
      this.selectedRegionImageWidget = selectedRegion.addChild(ImageWidget.sprite(10, 8, ServiceQuality.UNKNOWN.getIcon()));
      helper.addChild(selectedRegion);
      helper.addChild(
         Button.builder(Component.translatable("mco.configure.world.buttons.region_preference"), button -> this.openPreferenceSelector())
            .bounds(0, 0, 212, 20)
            .build()
      );
      helper.addChild(SpacerElement.height(2));
      this.closeOpenButton = helper.addChild(
         Button.builder(
               Component.empty(),
               button -> {
                  if (serverData.state == RealmsServer.State.OPEN) {
                     minecraft.gui
                        .setScreen(
                           RealmsPopups.customPopupScreen(
                              configurationScreen,
                              Component.translatable("mco.configure.world.close.question.title"),
                              Component.translatable("mco.configure.world.close.question.line1"),
                              popup -> {
                                 this.save();
                                 configurationScreen.closeTheWorld();
                              }
                           )
                        );
                  } else {
                     this.save();
                     configurationScreen.openTheWorld(false);
                  }
               }
            )
            .bounds(0, 0, 212, 20)
            .build()
      );
      this.closeOpenButton.active = false;
      this.updateData(serverData);
   }

   private static MutableComponent getTranslatableFromPreference(final RealmsSettingsTab.RegionSelection regionSelection) {
      return (regionSelection.preference().equals(RegionSelectionPreference.MANUAL) && regionSelection.region() != null
            ? Component.translatable(regionSelection.region().translationKey)
            : Component.translatable(regionSelection.preference().translationKey))
         .withStyle(ChatFormatting.GRAY);
   }

   private static Identifier getServiceQualityIcon(
      final RealmsSettingsTab.RegionSelection regionSelection, final Map<RealmsRegion, ServiceQuality> regionServiceQuality
   ) {
      if (regionSelection.region() != null && regionServiceQuality.containsKey(regionSelection.region())) {
         ServiceQuality serviceQuality = regionServiceQuality.getOrDefault(regionSelection.region(), ServiceQuality.UNKNOWN);
         return serviceQuality.getIcon();
      } else {
         return ServiceQuality.UNKNOWN.getIcon();
      }
   }

   private boolean isRealmNameValid() {
      String name = this.nameEdit.getValue();
      String trimmedName = name.trim();
      return !trimmedName.isEmpty() && name.length() == trimmedName.length();
   }

   private void openPreferenceSelector() {
      this.minecraft
         .gui
         .setScreen(
            new RealmsPreferredRegionSelectionScreen(
               this.configurationScreen, this::applyRegionPreferenceSelection, this.regionServiceQuality, this.preferredRegionSelection
            )
         );
   }

   private void applyRegionPreferenceSelection(final RegionSelectionPreference preference, final RealmsRegion region) {
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(preference, region);
      this.updateRegionPreferenceValues();
   }

   private void updateRegionPreferenceValues() {
      this.selectedRegionStringWidget.setMessage(getTranslatableFromPreference(this.preferredRegionSelection));
      this.selectedRegionImageWidget.updateResource(getServiceQualityIcon(this.preferredRegionSelection, this.regionServiceQuality));
      this.selectedRegionImageWidget.visible = this.preferredRegionSelection.preference == RegionSelectionPreference.MANUAL;
   }

   @Override
   public void onSelected(final RealmsServer serverData) {
      this.updateData(serverData);
   }

   @Override
   public void updateData(final RealmsServer serverData) {
      this.serverData = serverData;
      if (serverData.regionSelectionPreference == null) {
         serverData.regionSelectionPreference = RegionSelectionPreferenceDto.DEFAULT;
      }

      if (serverData.regionSelectionPreference.regionSelectionPreference == RegionSelectionPreference.MANUAL
         && serverData.regionSelectionPreference.preferredRegion == null) {
         Optional<RealmsRegion> first = this.regionServiceQuality.keySet().stream().findFirst();
         first.ifPresent(region -> serverData.regionSelectionPreference.preferredRegion = region);
      }

      String key = serverData.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close" : "mco.configure.world.buttons.open";
      this.closeOpenButton.setMessage(Component.translatable(key));
      this.closeOpenButton.active = true;
      this.preferredRegionSelection = new RealmsSettingsTab.RegionSelection(
         serverData.regionSelectionPreference.regionSelectionPreference, serverData.regionSelectionPreference.preferredRegion
      );
      this.nameEdit.setValue(Objects.requireNonNullElse(serverData.getName(), ""));
      this.descEdit.setValue(serverData.getDescription());
      this.updateRegionPreferenceValues();
   }

   @Override
   public void onDeselected(final RealmsServer serverData) {
      this.save();
   }

   public void save() {
      String realmName = this.nameEdit.getValue().trim();
      if (this.serverData.regionSelectionPreference == null
         || !Objects.equals(realmName, this.serverData.name)
         || !Objects.equals(this.descEdit.getValue(), this.serverData.motd)
         || this.preferredRegionSelection.preference() != this.serverData.regionSelectionPreference.regionSelectionPreference
         || this.preferredRegionSelection.region() != this.serverData.regionSelectionPreference.preferredRegion) {
         this.configurationScreen
            .saveSettings(realmName, this.descEdit.getValue(), this.preferredRegionSelection.preference(), this.preferredRegionSelection.region());
      }
   }

   public record RegionSelection(RegionSelectionPreference preference, @Nullable RealmsRegion region) {
   }
}
