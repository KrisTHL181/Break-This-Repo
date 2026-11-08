package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.button.*;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.resource.models.AuthorInfo;
import com.elfmcys.yesstevemodel.resource.models.Metadata;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Platform;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.gpu.GpuCapability;
import rip.ysm.pinyin.PinyinMatcher;

import java.util.*;

public class PlayerModelScreen extends Screen implements IGuiWidget {

    private static final String AUTHOR_SEARCH_PREFIX = "@";

    private static final String TAG_SEARCH_PREFIX = "#";

    private final HashSet<String> hiddenModels;

    private final Map<String, ModelPackData> modelPackMap;

    private Map<String, ModelAssembly> filteredModels;

    private Map<String, ModelPackData> filteredPacks;

    private List<String> sortedModelKeys;

    private List<String> sortedPackKeys;

    public int guiLeft;

    public int guiTop;

    private int maxPage;

    private EditBox searchBox;

    private SearchSuggestions suggestions;

    private Category category;

    private static final PlayerPreviewEntity[] previewHolders = new PlayerPreviewEntity[10];

    private static final Object2IntMap<String> pageIndexMap = new Object2IntOpenHashMap();

    private static String currentPath = StringPool.EMPTY;

    static {
        for (int i = 0; i < previewHolders.length; i++) {
            previewHolders[i] = new PlayerPreviewEntity();
        }
    }

    public PlayerModelScreen() {
        super(Component.literal("YSM Player Model GUI"));
        this.hiddenModels = Sets.newHashSet();
        this.filteredModels = Maps.newHashMap();
        this.filteredPacks = Maps.newHashMap();
        this.category = Category.ALL;
        if (NetworkHandler.isClientConnected()) {
            this.hiddenModels.addAll(ServerConfig.CLIENT_NOT_DISPLAY_MODELS.get());
        }
        ClientModelManager.registerGuiWidget(this);
        this.modelPackMap = new Object2ReferenceOpenHashMap<>(ClientModelManager.getModelPackMap());
    }

    public ModelButton createModelButton(int x, int y, boolean isAuthLocked, PlayerPreviewEntity previewEntity, ModelAssembly modelAssembly) {
        return new ModelButton(x, y, isAuthLocked, previewEntity, modelAssembly, previewEntity.getModelId());
    }

    public ModelButton createModelButton(int x, int y, boolean isAuthLocked, PlayerPreviewEntity previewEntity, ModelAssembly modelAssembly, String targetModelId) {
        return new ModelButton(x, y, isAuthLocked, previewEntity, modelAssembly, targetModelId);
    }

    public Screen createTextureScreen(PlayerModelScreen other, String str, ModelAssembly modelAssembly) {
        if (GeneralConfig.TEXTURE_SCREEN_MODE != null && GeneralConfig.TEXTURE_SCREEN_MODE.get() == GeneralConfig.TextureScreenMode.MODERN) {
            return new rip.ysm.gui.ModernPlayerTextureScreen(other, str, modelAssembly);
        }
        return new PlayerTextureScreen(other, str, modelAssembly);
    }

    public Screen createModelInfoScreen(PlayerModelScreen other, ModelAssembly modelAssembly) {
        if (GeneralConfig.MODEL_INFO_SCREEN_MODE != null && GeneralConfig.MODEL_INFO_SCREEN_MODE.get() == GeneralConfig.ModelInfoScreenMode.MODERN) {
            return new rip.ysm.gui.ModernModelInfoScreen(other, modelAssembly);
        }
        return new ModelInfoScreen(other, modelAssembly);
    }

    private Map<String, ModelAssembly> buildFilteredModelMap() {
        HashMap mapNewHashMap = Maps.newHashMap();
        if (StringUtils.isBlank(currentPath)) {
            mapNewHashMap.putAll(ClientModelManager.getModelAssemblyMap());
        }
        ClientModelManager.getModelAssemblyMap().forEach((str, modelAssembly) -> {
            if (str.startsWith(currentPath)) {
                mapNewHashMap.put(str, modelAssembly);
            }
            String str2 = FileTypeUtil.splitFileNameAndParentDir(str).right();
            if (StringUtils.isNotBlank(str2)) {
                ensurePackHierarchy(str2, this.modelPackMap);
            }
        });
        return mapNewHashMap;
    }

    private static void ensurePackHierarchy(String str, Map<String, ModelPackData> map) {
        if (StringUtils.isBlank(str) || !str.contains("/")) {
            return;
        }
        String[] strArrSplit = str.split("/");
        StringBuilder sb = new StringBuilder();
        for (String str2 : strArrSplit) {
            if (!str2.isEmpty()) {
                sb.append(str2).append("/");
                String string = sb.toString();
                map.putIfAbsent(string, new ModelPackData(string, FileTypeUtil.getFinalPathSegment(string), StringPool.EMPTY, null, null));
            }
        }
    }

    private Map<String, ModelPackData> buildFilteredPackMap() {
        HashMap<String, ModelPackData> mapNewHashMap = Maps.newHashMap();
        ClientModelManager.getModelAssemblyMap().keySet().forEach(str -> {
            String parent = FileTypeUtil.splitFileNameAndParentDir(str).right();
            if (StringUtils.isNotBlank(parent)) {
                ensurePackHierarchy(parent, this.modelPackMap);
            }
        });
        if (StringUtils.isBlank(currentPath)) {
            return Maps.newHashMap(this.modelPackMap);
        }
        this.modelPackMap.forEach((str, c0616x1389bc7f) -> {
            if (str.startsWith(currentPath)) {
                mapNewHashMap.put(str, c0616x1389bc7f);
            }
        });
        return mapNewHashMap;
    }

    private void refreshModelList() {
        String lowerCase;
        this.filteredModels = Maps.newHashMap();
        this.filteredPacks = Maps.newHashMap();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        LocalPlayer localPlayer = this.minecraft.player;
        if (this.category == Category.ALL) {
            this.filteredModels = buildFilteredModelMap();
            this.filteredPacks = buildFilteredPackMap();
        }
        if (this.category == Category.AUTH) {
            Optional<AuthModelsCapability> authCap = AuthModelsCapability.get(localPlayer);
            boolean allowAll = ClientOnlyMode.isActive();
            for (Map.Entry<String, ModelAssembly> entry : ClientModelManager.getModelAssemblyMap().entrySet()) {
                boolean owned = authCap.map(cap -> cap.containsModel(entry.getKey())).orElse(false);
                if (allowAll || owned || !entry.getValue().getTextureRegistry().isAuthModel()) {
                    this.filteredModels.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (this.category == Category.STAR) {
            StarModelsCapability.get(localPlayer).ifPresent(cap2 -> {
                for (Map.Entry<String, ModelAssembly> entry : ClientModelManager.getModelAssemblyMap().entrySet()) {
                    if (cap2.containsModel(entry.getKey())) {
                        this.filteredModels.put(entry.getKey(), entry.getValue());
                    }
                }
            });
        }
        if (this.searchBox != null) {
            lowerCase = this.searchBox.getValue().toLowerCase(Locale.ENGLISH);
        } else {
            lowerCase = StringPool.EMPTY;
        }
        if (StringUtils.isBlank(lowerCase)) {
            this.filteredModels.entrySet().removeIf(entry -> {
                Pair<String, String> pair = FileTypeUtil.splitFileNameAndParentDir(entry.getKey());
                return this.hiddenModels.contains(pair.left()) || !pair.right().equals(currentPath);
            });
            this.filteredPacks.entrySet().removeIf(entry2 -> {
                return !isDirectChild(currentPath, entry2.getKey());
            });
        } else {
            String str = lowerCase;
            this.filteredModels.entrySet().removeIf(entry3 -> {
                return shouldFilterModel(FileTypeUtil.splitFileNameAndParentDir(entry3.getKey()).left(), entry3.getValue(), str);
            });
            String str2 = lowerCase;
            this.filteredPacks.entrySet().removeIf(entry4 -> {
                return shouldFilterPack(FileTypeUtil.getFinalPathSegment(entry4.getKey()), entry4.getValue(), str2);
            });
        }
        this.sortedModelKeys = Lists.newArrayList(this.filteredModels.keySet());
        this.sortedModelKeys.sort((v0, v1) -> {
            return v0.compareTo(v1);
        });
        this.sortedPackKeys = Lists.newArrayList(this.filteredPacks.keySet());
        this.sortedPackKeys.sort((v0, v1) -> {
            return v0.compareTo(v1);
        });
        this.maxPage = ((this.filteredModels.size() + this.filteredPacks.size()) - 1) / 10;
    }

    private boolean isDirectChild(String str, String str2) {
        String strSubstring;
        int iIndexOf;
        if (str.equals(str2)) {
            return false;
        }
        if (!StringUtils.isBlank(str)) {
            return str2.startsWith(str) && (iIndexOf = (strSubstring = str2.substring(str.length())).indexOf(47)) == strSubstring.length() - 1 && strSubstring.lastIndexOf(47) == iIndexOf;
        }
        int iIndexOf2 = str2.indexOf(47);
        return iIndexOf2 == str2.length() - 1 && str2.lastIndexOf(47) == iIndexOf2;
    }

    private boolean shouldFilterPack(String str, ModelPackData packData, String str2) {
        if (StringUtils.isBlank(str2)) {
            return false;
        }
        if (str2.startsWith(TAG_SEARCH_PREFIX)) {
            str2 = str2.substring(TAG_SEARCH_PREFIX.length());
        }
        if (PinyinMatcher.contains(str, str2)) {
            return false;
        }
        if (packData.getTranslations() != null) {
            if (PinyinMatcher.contains(ModelMetadataPresenter.getLocalizedString(packData, "name", packData.getName()), str2)) {
                return false;
            }
            String str3 = packData.getDescription();
            return str3 == null || !PinyinMatcher.contains(ModelMetadataPresenter.getLocalizedString(packData, "description", str3), str2);
        }
        return true;
    }

    private boolean shouldFilterModel(String str, ModelAssembly modelAssembly, String str2) {
        if (this.hiddenModels.contains(str)) {
            return true;
        }
        if (StringUtils.isBlank(str2)) {
            return false;
        }
        if (str2.startsWith(TAG_SEARCH_PREFIX)) {
            return true;
        }
        if (str2.startsWith(AUTHOR_SEARCH_PREFIX)) {
            String strSubstring = str2.substring(AUTHOR_SEARCH_PREFIX.length());
            Metadata metadata2 = modelAssembly.getModelData().getExtraInfo();
            if (metadata2 != null) {
                return matchesAuthorSearch(modelAssembly, strSubstring, metadata2);
            }
            return true;
        }
        if (PinyinMatcher.contains(str, str2)) {
            return false;
        }
        Metadata metadata3 = modelAssembly.getModelData().getExtraInfo();
        if (metadata3 != null) {
            if (PinyinMatcher.contains(ModelMetadataPresenter.getLocalizedModelString(modelAssembly, "metadata.name", metadata3.getName()), str2) || PinyinMatcher.contains(ModelMetadataPresenter.getLocalizedModelString(modelAssembly, "metadata.tips", metadata3.getTips()), str2)) {
                return false;
            }
            return matchesAuthorSearch(modelAssembly, str2, metadata3);
        }
        return true;
    }

    public String getParentPath(String str) {
        if (str == null || str.isEmpty()) {
            return StringPool.EMPTY;
        }
        String strSubstring = str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
        int iLastIndexOf = strSubstring.lastIndexOf(47);
        if (iLastIndexOf < 0) {
            return StringPool.EMPTY;
        }
        return strSubstring.substring(0, iLastIndexOf + 1);
    }

    private boolean matchesAuthorSearch(ModelAssembly modelAssembly, String str, Metadata metadata2) {
        int i = 0;
        Iterator<AuthorInfo> it = metadata2.getAuthors().iterator();
        while (it.hasNext()) {
            if (PinyinMatcher.contains(ModelMetadataPresenter.getLocalizedModelString(modelAssembly, "metadata.authors.%d.name".formatted(Integer.valueOf(i)), it.next().getName()), str)) {
                return false;
            }
            i++;
        }
        return true;
    }

    public void init() {
        clearWidgets();
        refreshModelList();
        if (getCurrentPage() > this.maxPage) {
            resetCurrentPage();
        }
        this.guiLeft = (this.width - 420) / 2;
        this.guiTop = (this.height - 235) / 2;
        String value = StringPool.EMPTY;
        boolean zIsFocused = false;
        if (this.searchBox != null) {
            value = this.searchBox.getValue();
            zIsFocused = this.searchBox.isFocused();
        }
        this.searchBox = new EditBox(Minecraft.getInstance().font, this.guiLeft + 144, this.guiTop + 6, 140, 16, Component.literal("YSM Search Box"));
        this.searchBox.setValue(value);
        this.searchBox.setTextColor(15986656);
        this.searchBox.setFocused(zIsFocused);
        this.searchBox.moveCursorToEnd();
        this.suggestions = new SearchSuggestions(this.font, this.searchBox, this.modelPackMap, this.suggestions);
        this.suggestions.refresh();
        addWidget(this.searchBox);
        addRenderableWidget(new IconButton(this.guiLeft + 5, this.guiTop + 5, 20, 20, 80, 16, button -> {
            if (Minecraft.getInstance().player != null) {
                PlayerCapability.get(Minecraft.getInstance().player).ifPresent(cap -> {
                    ModelAssembly modelAssembly = cap.getModelAssembly();
                    if (modelAssembly.getModelData().getExtraInfo() != null) {
                        Minecraft.getInstance().setScreen(createModelInfoScreen(this, modelAssembly));
                    }
                });
            }
        })).setTooltipText("gui.yes_steve_model.model.info");
        addRenderableWidget(new IconButton(this.guiLeft + 28, this.guiTop + 5, 79, 20, 32, 16, button2 -> {
            if (Minecraft.getInstance().player != null) {
                PlayerCapability.get(Minecraft.getInstance().player).ifPresent(cap -> {
                    Minecraft.getInstance().setScreen(createTextureScreen(this, cap.getModelId(), cap.getModelAssembly()));
                });
            }
        }).setTooltipText("gui.yes_steve_model.model.texture"));
        addRenderableWidget(new ModIconButton(this.guiLeft + 110, this.guiTop + 5));
        if (StringUtils.isNotBlank(currentPath)) {
            addRenderableWidget(new IconButton(this.guiLeft + 110, this.guiTop + 27, 20, 20, 0, 32, button3 -> {
                navigateUp();
            }).setTooltipText("gui.back"));
        }
        addRenderableWidget(new Checkbox(this.guiLeft + 5, this.guiTop - 22, 20, 20, Component.translatable("gui.yes_steve_model.show_model_id_first"), GeneralConfig.SHOW_MODEL_ID_FIRST.get(), true) {
            public void onPress() {
                super.onPress();
                GeneralConfig.SHOW_MODEL_ID_FIRST.set(selected());
                GeneralConfig.SHOW_MODEL_ID_FIRST.save();
            }
        });
        addRenderableWidget(new IconButton(this.guiLeft + 328, this.guiTop + 5, 18, 18, 32, 0, button4 -> {
            if (this.category != Category.ALL) {
                this.category = Category.ALL;
                resetCurrentPage();
                init();
            }
        }).setTooltipText("gui.yes_steve_model.all_models"));
        addRenderableWidget(new IconButton(this.guiLeft + 308, this.guiTop + 5, 18, 18, 48, 0, button5 -> {
            if (this.category != Category.AUTH) {
                this.category = Category.AUTH;
                resetCurrentPage();
                init();
            }
        }).setTooltipText("gui.yes_steve_model.auth_models"));
        addRenderableWidget(new IconButton(this.guiLeft + 288, this.guiTop + 5, 18, 18, 0, 0, button6 -> {
            if (this.category != Category.STAR) {
                this.category = Category.STAR;
                resetCurrentPage();
                init();
            }
        }).setTooltipText("gui.yes_steve_model.star_models"));
        addRenderableWidget(new IconButton(this.guiLeft + 397, this.guiTop + 5, 18, 18, 16, 16, button7 -> {
            Minecraft.getInstance().setScreen(new ExtraPlayerConfigScreen(this));
        }).setTooltipText("gui.yes_steve_model.config"));
        boolean canUpload = ClientModelManager.isAllowUpload() && ClientModelManager.isOysmServer();
        IconButton uploadButton = new IconButton(this.guiLeft + 377, this.guiTop + 5, 18, 18, 0, 16, button8 -> {
            Minecraft.getInstance().setScreen(new ModelUploadScreen(this));
        });
        uploadButton.active = canUpload;
        uploadButton.setTooltipLines(java.util.Collections.singletonList(Component.literal(canUpload ? "Upload model to server" : "Server has uploads disabled, or this is not an OpenYSM server")));
        addRenderableWidget(uploadButton);
        addRenderableWidget(new IconButton(this.guiLeft + 357, this.guiTop + 5, 18, 18, 80, 0, button9 -> {
            Minecraft.getInstance().setScreen(new OpenModelFolderScreen(this));
        }).setTooltipText("gui.yes_steve_model.open_model_folder.open"));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 198, this.guiTop + 215, 52, 14, Component.translatable("gui.yes_steve_model.pre_page"), button10 -> {
            int currentPage = getCurrentPage();
            if (currentPage > 0) {
                setCurrentPage(currentPage - 1);
                init();
            }
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 308, this.guiTop + 215, 52, 14, Component.translatable("gui.yes_steve_model.next_page"), button11 -> {
            int currentPage = getCurrentPage();
            if (currentPage < this.maxPage) {
                setCurrentPage(currentPage + 1);
                init();
            }
        }));
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        Optional<AuthModelsCapability> capability = AuthModelsCapability.get(this.minecraft.player);
        for (int i = 0; i < 10; i++) {
            int slotIndex = i + (getCurrentPage() * 10);
            int slotX = this.guiLeft + 143 + (55 * (i % 5));
            int slotY = this.guiTop + 28 + (93 * (i / 5));
            if (slotIndex < this.sortedPackKeys.size()) {
                String str = this.sortedPackKeys.get(slotIndex);
                getPackData(str).ifPresent(value2 -> {
                    addRenderableWidget(new PackIconButton(slotX, slotY, 52, 90, value2, button12 -> {
                        currentPath = str;
                        clearSearch();
                        resetCurrentPage();
                        init();
                    }));
                });
            }
            int size = slotIndex - this.sortedPackKeys.size();
            if (0 <= size && size < this.sortedModelKeys.size()) {
                String str2 = this.sortedModelKeys.get(size);
                PlayerPreviewEntity previewEntity = previewHolders[i];
                previewEntity.resetModel();
                ModelAssembly modelAssembly2 = this.filteredModels.get(str2);
                if (modelAssembly2 != null) {
                    boolean isAuthLocked = !ClientOnlyMode.isActive() && modelAssembly2.getTextureRegistry().isAuthModel() && capability.map(cap -> !cap.getAuthModels().contains(str2)).orElse(true);
                    if (!ClientModelManager.isModelPending(str2)) {
                        previewEntity.initModelWithTexture(str2, modelAssembly2.getAnimationBundle().getDefaultTextureName());
                        previewEntity.getAnimationStateMachine().setCurrentAnimation(modelAssembly2.getModelData().getModelProperties().getPreviewAnimation());
                    }
                    addRenderableWidget(createModelButton(slotX, slotY, isAuthLocked, previewEntity, modelAssembly2, str2));
                }
            }
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.fillGradient(this.guiLeft, this.guiTop, this.guiLeft + 135, this.guiTop + 235, -14540254, -14540254);
        guiGraphics.fillGradient(this.guiLeft + 138, this.guiTop, this.guiLeft + 420, this.guiTop + 235, -14540254, -14540254);
        guiGraphics.fillGradient(this.guiLeft + 351, this.guiTop + 7, this.guiLeft + 352, this.guiTop + 21, -790560, -790560);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
        renderModelPreview(guiGraphics, mouseX, mouseY, this.minecraft.getFrameTime());
        if (this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
            guiGraphics.drawString(this.font, Component.translatable("gui.yes_steve_model.search").withStyle(ChatFormatting.ITALIC), this.guiLeft + 148, this.guiTop + 10, 7829367);
        }
        String str = String.format("%d/%d", getCurrentPage() + 1, Integer.valueOf(this.maxPage + 1));
        Font font = this.font;
        int iWidth = this.guiLeft + 138 + ((282 - this.font.width(str)) / 2);
        int pageY = this.guiTop + 223;
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(font, str, iWidth, pageY - (9 / 2), 15986656);
        String renderer = (NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get()) ? "SIMD" : "Fallback";
        if(renderer.equals("SIMD") && GpuCapability.isAvailable() && GeneralConfig.USE_GPU_RENDERER.get()) {
            renderer = "GPU";
        }
        String strVersionString = Platform.getMod(YesSteveModel.MOD_ID).getVersion();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 1000.0f);
        guiGraphics.drawString(this.font, strVersionString + " (" + renderer + ")", this.guiLeft + 2, this.guiTop + 226, ChatFormatting.DARK_GRAY.getColor().intValue());
        guiGraphics.pose().popPose();
        renderBreadcrumb(guiGraphics, mouseX, mouseY);
        renderSyncStatus(guiGraphics);
        boolean occluded = this.suggestions != null && this.suggestions.isOccluding(mouseX, mouseY);
        int hoverX = occluded ? -1000 : mouseX;
        int hoverY = occluded ? -1000 : mouseY;
        super.render(guiGraphics, hoverX, hoverY, partialTick);
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable -> {
            return renderable instanceof IconButton;
        }).forEach(renderable2 -> {
            ((IconButton) renderable2).renderTooltip(guiGraphics, this, hoverX, hoverY);
        });
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable3 -> {
            return renderable3 instanceof ModelButton;
        }).forEach(renderable4 -> {
            ((ModelButton) renderable4).renderTooltip(guiGraphics, this, hoverX, hoverY);
        });
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable5 -> {
            return renderable5 instanceof PackIconButton;
        }).forEach(renderable6 -> {
            ((PackIconButton) renderable6).renderDescription(guiGraphics, this, hoverX, hoverY);
        });
        if (this.suggestions != null) {
            this.suggestions.render(guiGraphics);
        }
        if (this.searchBox.isHovered() && (this.suggestions == null || !this.suggestions.isVisible())) {
            MutableComponent mutableComponentWithStyle = Component.translatable("gui.yes_steve_model.search.tip").withStyle(ChatFormatting.GRAY);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, 0.0f, 4000.0f);
            guiGraphics.renderTooltip(this.font, this.font.split(mutableComponentWithStyle, 320), mouseX, mouseY);
            guiGraphics.pose().popPose();
        }
    }

    private List<BreadcrumbSegment> buildBreadcrumb() {
        List<BreadcrumbSegment> segments = Lists.newArrayList();
        if (StringUtils.isBlank(currentPath)) {
            return segments;
        }
        int x = this.guiLeft + 142;
        int y = this.guiTop - 12;
        segments.add(new BreadcrumbSegment("📂", StringPool.EMPTY, x, y, this.font.width("📂")));
        x += this.font.width("📂 ");
        StringBuilder path = new StringBuilder();
        for (String part : currentPath.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            path.append(part).append("/");
            int width = this.font.width(part);
            segments.add(new BreadcrumbSegment(part, path.toString(), x, y, width));
            x += width;
            x += this.font.width(" / ");
        }
        return segments;
    }

    private void renderBreadcrumb(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<BreadcrumbSegment> segments = buildBreadcrumb();
        for (int i = 0; i < segments.size(); i++) {
            BreadcrumbSegment segment = segments.get(i);
            boolean hovered = segment.isClickable() && segment.contains(mouseX, mouseY);
            guiGraphics.drawString(this.font, segment.label, segment.x, segment.y, hovered ? 16777120 : 15986656);
            if (hovered) {
                guiGraphics.fill(segment.x, segment.y + 9, segment.x + segment.width, segment.y + 10, 0xFFFFFF60);
            }
            if (i > 0 && i < segments.size() - 1) {
                guiGraphics.drawString(this.font, "/", segment.x + segment.width + this.font.width(" "), segment.y, 7829367);
            }
        }
    }

    private void clearSearch() {
        if (this.searchBox != null) {
            this.searchBox.setValue(StringPool.EMPTY);
            this.searchBox.setFocused(false);
        }
        if (this.suggestions != null) {
            this.suggestions.suppress();
        }
    }

    private void navigateToSuggestedPack() {
        if (this.suggestions == null) {
            return;
        }
        String packPath = this.suggestions.consumePendingPackPath();
        if (packPath != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            currentPath = packPath;
            clearSearch();
        }
    }

    private boolean breadcrumbClicked(double mouseX, double mouseY) {
        for (BreadcrumbSegment segment : buildBreadcrumb()) {
            if (segment.isClickable() && segment.contains(mouseX, mouseY) && !segment.targetPath.equals(currentPath)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                currentPath = segment.targetPath;
                resetCurrentPage();
                init();
                return true;
            }
        }
        return false;
    }

    private static final class BreadcrumbSegment {
        private final String label;
        private final String targetPath;
        private final int x;
        private final int y;
        private final int width;

        private BreadcrumbSegment(String label, String targetPath, int x, int y, int width) {
            this.label = label;
            this.targetPath = targetPath;
            this.x = x;
            this.y = y;
            this.width = width;
        }

        private boolean isClickable() {
            return !this.targetPath.isEmpty() && !this.targetPath.equals(currentPath);
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 9;
        }
    }

    private void renderSyncStatus(GuiGraphics guiGraphics) {
        MutableComponent mutableComponentLiteral;
        ClientModelManager.SyncStatus currentState = ClientModelManager.getSyncStatus();
        switch (currentState.getCurrentState()) {
            case WAITING:
                mutableComponentLiteral = Component.translatable("gui.yes_steve_model.sync_hint.waiting");
                break;
            case LOADING:
                mutableComponentLiteral = Component.translatable("gui.yes_steve_model.sync_hint.loading");
                break;
            case PREPARING:
                mutableComponentLiteral = Component.translatable("gui.yes_steve_model.sync_hint.preparing");
                break;
            case SYNCING:
                if (currentState.getSyncedModels() == 0) {
                    mutableComponentLiteral = Component.translatable("gui.yes_steve_model.sync_hint.syncing");
                    break;
                } else {
                    mutableComponentLiteral = Component.literal(String.format("%s/%s", currentState.getSyncedModels(), currentState.getTotalModels()));
                    break;
                }
            default:
                return;
        }
        int iWidth = (this.guiLeft + 414) - this.font.width(mutableComponentLiteral);
        int i = this.guiTop + 215;
        Objects.requireNonNull(this.font);
        guiGraphics.drawString(this.font, mutableComponentLiteral, iWidth, i + Math.round((14 - 9) / 2.0f), ChatFormatting.DARK_GRAY.getColor().intValue());
    }

    public void renderModelPreview(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            RenderSystem.enableScissor((int) ((this.guiLeft + 5) * guiScale), (int) (Minecraft.getInstance().getWindow().getHeight() - ((this.guiTop + 200) * guiScale)), (int) (125.0d * guiScale), (int) (171.0d * guiScale));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0f, 0.0f, 100.0f);
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, this.guiLeft + 67, this.guiTop + 190, 70, (this.guiLeft + 67) - mouseX, ((this.guiTop + 180) - 95) - mouseY, localPlayer);
            guiGraphics.pose().popPose();
            RenderSystem.disableScissor();
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                List<FormattedCharSequence> listSplit = this.font.split(FormattedText.of(ClientModelManager.getModelContext(cap.getModelId()).map(it -> {
                    Metadata metadata2 = it.getModelData().getExtraInfo();
                    if (metadata2 != null) {
                        return ModelMetadataPresenter.getLocalizedModelString(it, "metadata.name", metadata2.getName());
                    }
                    return StringPool.EMPTY;
                }).filter(charSequence -> {
                    return StringUtils.isNoneBlank(charSequence);
                }).orElse(FileTypeUtil.getNameWithoutArchiveExtension(cap.getModelId()))), 125);
                int lineY = this.guiTop + 205;
                for (FormattedCharSequence formattedCharSequence : listSplit) {
                    guiGraphics.drawString(this.font, formattedCharSequence, this.guiLeft + ((135 - this.font.width(formattedCharSequence)) / 2), lineY, 15986656);
                    lineY += 10;
                }
            });
        }
    }

    public void resize(Minecraft minecraft, int width, int height) {
        String value = this.searchBox.getValue();
        super.resize(minecraft, width, height);
        this.searchBox.setValue(value);
    }

    public void tick() {
        this.searchBox.tick();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.suggestions != null && this.suggestions.mouseClicked(mouseX, mouseY)) {
            navigateToSuggestedPack();
            resetCurrentPage();
            init();
            return true;
        }
        if (button == 0 && breadcrumbClicked(mouseX, mouseY)) {
            return true;
        }
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(this.searchBox);
            return true;
        }
        if (this.searchBox.isFocused()) {
            this.searchBox.setFocused(false);
            if (this.suggestions != null) {
                this.suggestions.suppress();
            }
        }
        boolean zMouseClicked = super.mouseClicked(mouseX, mouseY, button);
        if (!zMouseClicked && button == 1 && StringUtils.isNotBlank(currentPath)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            navigateUp();
            zMouseClicked = true;
        }
        return zMouseClicked;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox == null) {
            return false;
        }
        String value = this.searchBox.getValue();
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            if (!Objects.equals(value, this.searchBox.getValue())) {
                resetCurrentPage();
                init();
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleToggleKey(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_F && Screen.hasControlDown()) {
            toggleSearchFocus();
            return true;
        }
        if (this.searchBox.isFocused() && this.suggestions != null && this.suggestions.keyPressed(keyCode)) {
            navigateToSuggestedPack();
            resetCurrentPage();
            init();
            return true;
        }
        boolean zIsPresent = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
        String value = this.searchBox.getValue();
        if (zIsPresent) {
            return true;
        }
        if (!this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return (this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != 256) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (!Objects.equals(value, this.searchBox.getValue())) {
            resetCurrentPage();
            init();
            return true;
        }
        return true;
    }

    private void toggleSearchFocus() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        if (this.searchBox.isFocused()) {
            clearSearch();
            resetCurrentPage();
            init();
            return;
        }
        setFocused(this.searchBox);
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd();
    }

    private boolean handleToggleKey(int keyCode, int scanCode, int modifiers) {
        if (PlayerModelToggleKey.KEY_MAPPING.matches(keyCode, scanCode) && !this.searchBox.isFocused()) {
            onClose();
            return true;
        }
        return false;
    }

    public void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.searchBox.setValue(text);
        } else {
            this.searchBox.insertText(text);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.minecraft == null) {
            return false;
        }
        if (this.suggestions != null && this.suggestions.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (delta != 0.0d && isInModelArea(mouseX, mouseY)) {
            return handleScrollPage(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isInModelArea(double mouseX, double mouseY) {
        return ((((double) (this.guiLeft + 143)) > mouseX ? 1 : (((double) (this.guiLeft + 143)) == mouseX ? 0 : -1)) < 0 && (mouseX > ((double) (this.guiLeft + 430)) ? 1 : (mouseX == ((double) (this.guiLeft + 430)) ? 0 : -1)) < 0) && ((((double) (this.guiTop + 25)) > mouseY ? 1 : (((double) (this.guiTop + 25)) == mouseY ? 0 : -1)) < 0 && (mouseY > ((double) (this.guiTop + 235)) ? 1 : (mouseY == ((double) (this.guiTop + 235)) ? 0 : -1)) < 0);
    }

    private void navigateUp() {
        String str2 = getParentPath(currentPath);
        if (!currentPath.equals(str2)) {
            String str = currentPath;
            currentPath = str2;
            pageIndexMap.removeInt(str);
            init();
        }
    }

    private boolean handleScrollPage(double delta) {
        int currentPage = getCurrentPage();
        if (delta > 0.0d && currentPage > 0) {
            setCurrentPage(currentPage - 1);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
        }
        if (delta < 0.0d && currentPage < this.maxPage) {
            setCurrentPage(currentPage + 1);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            init();
            return true;
        }
        return true;
    }

    public int getCurrentPage() {
        return pageIndexMap.getOrDefault(currentPath, 0);
    }

    public void setCurrentPage(int i) {
        pageIndexMap.put(currentPath, i);
    }

    public void resetCurrentPage() {
        pageIndexMap.put(currentPath, 0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onModelsLoaded(Map<String, ModelAssembly> map) {
        init();
    }

    @Override
    public void onModelsUpdated(Map<String, ModelAssembly> map) {
        init();
    }

    private Optional<ModelPackData> getPackData(String str) {
        return Optional.ofNullable(this.modelPackMap.get(str));
    }

    private enum Category {
        ALL,
        AUTH,
        STAR
    }
}