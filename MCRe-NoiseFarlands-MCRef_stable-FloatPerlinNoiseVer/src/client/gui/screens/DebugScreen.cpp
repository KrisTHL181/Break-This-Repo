#include "DebugScreen.h"
#include "../components/Button.h"
#include "../../Minecraft.h"
#include "../../player/LocalPlayer.h"
#include "../../../world/level/Level.h"
#include "../../../world/entity/MobFactory.h"
#include "../../../world/level/MobSpawner.h"
#include "../../../world/entity/player/Inventory.h"
#include "PrerenderTilesScreen.h"
#include "ArmorScreen.h"
#include "../../gamemode/GameMode.h"
#include "../../renderer/Textures.h"
#include "../../Options.h"
#include "../../../network/packet/AdventureSettingsPacket.h"
#include "../../../network/RakNetInstance.h"
#include "../../sound/SoundEngine.h"
#include "../../../world/Difficulty.h"
#include "../../../util/PerfRenderer.h"
#include "../../../util/PerfTimer.h"
#include "../../../client/gui/Gui.h"

DebugScreen::DebugScreen(Minecraft* mc)
    : mc(mc)
{
    for (int i = 0; i < 10; ++i)
        digitButtons[i] = nullptr;
}

DebugScreen::~DebugScreen()
{
    for (int i = 0; i < 10; ++i) delete digitButtons[i];
    for (auto* b : extraButtons) delete b;
}

void DebugScreen::init()
{
    // 数字按钮 0-9
    for (int i = 0; i < 10; ++i)
        addDigitButton(i);

    // 后退按钮
    Button* backBtn = new Button(ACT_BACK, "< Back");
    buttons.push_back(backBtn);
    extraButtons.push_back(backBtn);

    // 额外功能按钮
    Button* b;
    b = new Button(ACT_HEAL_RESET, "Heal"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_TOGGLE_GAMEMODE, "Gamemode"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_ADVANCE_TIME, "Time +"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_OPEN_ARMOR, "Armor"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_HURT_RELOAD, "Hurt+Reload"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_SPAWN_MOB, "Spawn Mob"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_MASSACRE, "Kill All"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_REFILL_INV, "Refill Inv"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_PRERENDER, "PreRender"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_DROP_INV, "Drop All"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_TOGGLE_DIFFICULTY, "Toggle Diff"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_TOGGLE_3RDPERSON, "3rd Person"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_SPEEDUP, "Speed Up"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_NOPVP, "No PvP"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_NOPVM, "No PvM"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_NOMVP, "No MvP"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_IMMUTABLE, "Immutable"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_NAMETAGS, "NameTags"); buttons.push_back(b); extraButtons.push_back(b);
    b = new Button(ACT_PARTICLES, "Particles"); buttons.push_back(b); extraButtons.push_back(b);

    Button* closeBtn = new Button(99, "Close");
    buttons.push_back(closeBtn);

    passEvents = false;
}

void DebugScreen::addDigitButton(int digit)
{
    char label[2] = { (char)('0' + digit), 0 };
    Button* btn = new Button(digit, label);
    buttons.push_back(btn);
    digitButtons[digit] = btn;
}

void DebugScreen::setupPositions()
{
    if (width <= 0 && mc && mc->width > 0 && Gui::InvGuiScale > 0)
        width = (int)(mc->width * Gui::InvGuiScale);
    if (height <= 0 && mc && mc->height > 0 && Gui::InvGuiScale > 0)
        height = (int)(mc->height * Gui::InvGuiScale);

    int cols = 5;
    if (width < 450) cols = 4;
    if (width < 360) cols = 3;

    int btnWidth = (width - 10) / cols - 4;
    int btnHeight = 28;
    int padX = 4, padY = 4;
    int startY = 90;

    std::vector<Button*> allBtns;
    for (int i = 0; i < 10; ++i) allBtns.push_back(digitButtons[i]);
    for (auto* b : extraButtons) allBtns.push_back(b);

    Button* closeBtn = nullptr;
    for (auto* b : buttons) {
        if (b->id == 99) { closeBtn = b; break; }
    }

    int total = (int)allBtns.size();
    int rows = (total + cols - 1) / cols;
    int gridWidth = btnWidth * cols + padX * (cols - 1);
    int startX = (width - gridWidth) / 2;

    for (int i = 0; i < total; ++i) {
        int row = i / cols, col = i % cols;
        allBtns[i]->width  = btnWidth;
        allBtns[i]->height = btnHeight;
        allBtns[i]->x = startX + col * (btnWidth + padX);
        allBtns[i]->y = startY + row * (btnHeight + padY);
    }

    if (closeBtn) {
        closeBtn->width  = 120;
        closeBtn->height = 30;
        int lastY = startY + rows * (btnHeight + padY);
        closeBtn->x = (width - closeBtn->width) / 2;
        closeBtn->y = lastY + 10;
    }

    int totalHeight = closeBtn ? (closeBtn->y + closeBtn->height + 10) : 0;
    if (totalHeight > height) {
        float scale = (float)(height - 30) / totalHeight;
        if (scale < 1.0f) {
            for (int i = 0; i < total; ++i) {
                int oldY = allBtns[i]->y;
                allBtns[i]->y = (int)(startY + (oldY - startY) * scale);
                allBtns[i]->height = (int)(allBtns[i]->height * scale);
            }
            if (closeBtn) {
                int oldY = closeBtn->y;
                closeBtn->y = (int)(startY + (oldY - startY) * scale);
                closeBtn->height = (int)(closeBtn->height * scale);
            }
        }
    }
}

void DebugScreen::render(int xm, int ym, float a)
{
    fill(0, 0, width, height, 0x30000000);
    drawCenteredString(mc->font, "Debug Panel", width / 2, 20, 0xFFFFFFFF);
    Screen::render(xm, ym, a);
}

void DebugScreen::keyPressed(int key)
{
    if (key == 27) { mc->setScreen(NULL); return; }

    if (key == 8) {   // Backspace 后退
        PerfRenderer* pr = mc->getPerfRenderer();
        if (pr) pr->debugFpsMeterKeyPress(0);
        return;
    }

    if (key >= '0' && key <= '9') {
        PerfRenderer* pr = mc->getPerfRenderer();
        if (pr) pr->debugFpsMeterKeyPress(key - '0');
        return;
    }
}

void DebugScreen::buttonClicked(Button* button)
{
    int id = button->id;
    if (id == 99) { mc->setScreen(NULL); return; }

    if (id == ACT_BACK) {
        PerfRenderer* pr = mc->getPerfRenderer();
        if (pr) pr->debugFpsMeterKeyPress(0);
        return;
    }

    if (id >= 0 && id <= 9) {
        PerfRenderer* pr = mc->getPerfRenderer();
        if (pr) pr->debugFpsMeterKeyPress(id);
        return;
    }

    executeExtraAction(id);
    if (id != ACT_OPEN_ARMOR && id != ACT_PRERENDER) mc->setScreen(NULL);
}

void DebugScreen::executeExtraAction(int id) {
    switch (id) {
        case ACT_HEAL_RESET:          mc->onGraphicsReset(); mc->player->heal(100); break;
        case ACT_TOGGLE_GAMEMODE:     mc->setIsCreativeMode(!mc->isCreativeMode()); break;
        case ACT_ADVANCE_TIME:        if (mc->level) mc->level->setTime(mc->level->getTime() + 1000); break;
        case ACT_OPEN_ARMOR:          mc->setScreen(new ArmorScreen()); break;
        case ACT_HURT_RELOAD:         mc->textures->reloadAll(); mc->player->hurtTo(2); break;
        case ACT_SPAWN_MOB: {
            Mob* mob = nullptr;
            int types[] = {MobTypes::Sheep, MobTypes::Pig, MobTypes::Chicken, MobTypes::Cow};
            mob = MobFactory::CreateMob(types[Mth::random(4)], mc->level);
            float dx = 4 - 8 * Mth::random() + 4 * Mth::sin(Mth::DEGRAD * mc->player->yRot);
            float dz = 4 - 8 * Mth::random() + 4 * Mth::cos(Mth::DEGRAD * mc->player->yRot);
            if (mob && !MobSpawner::addMob(mc->level, mob, mc->player->x + dx, mc->player->y, mc->player->z + dz,
                                           Mth::random() * 360, 0, true))
                delete mob;
            break;
        }
        case ACT_MASSACRE: {
            const EntityList& entities = mc->level->getAllEntities();
            for (int i = entities.size() - 1; i >= 0; --i) {
                Entity* e = entities[i];
                if (!e->isPlayer()) mc->level->removeEntity(e);
            }
            break;
        }
        case ACT_REFILL_INV:          mc->player->inventory->clearInventoryWithDefault(); break;
        case ACT_PRERENDER:           mc->setScreen(new PrerenderTilesScreen()); break;
        case ACT_DROP_INV:
            for (int i = Inventory::MAX_SELECTION_SIZE; i < mc->player->inventory->getContainerSize(); ++i)
                if (mc->player->inventory->getItem(i))
                    mc->player->inventory->dropSlot(i, false);
            break;
        case ACT_TOGGLE_DIFFICULTY: {
            Difficulty diff = (Difficulty)mc->options.getIntValue(OPTIONS_DIFFICULTY);
            diff = (diff == Difficulty::PEACEFUL) ? Difficulty::NORMAL : Difficulty::PEACEFUL;
            mc->options.set(OPTIONS_DIFFICULTY, diff);
            mc->level->difficulty = diff;
            break;
        }
        case ACT_TOGGLE_3RDPERSON:    mc->options.toggle(OPTIONS_THIRD_PERSON_VIEW); break;
        case ACT_SPEEDUP:             for (int i = 0; i < 5 * SharedConstants::TicksPerSecond; ++i) mc->level->tick(); break;
        case ACT_NOPVP:              { auto& as = mc->level->adventureSettings; as.noPvP = !as.noPvP; AdventureSettingsPacket p(as); mc->raknetInstance->send(p); break; }
        case ACT_NOPVM:              { auto& as = mc->level->adventureSettings; as.noPvM = !as.noPvM; AdventureSettingsPacket p(as); mc->raknetInstance->send(p); break; }
        case ACT_NOMVP:              { auto& as = mc->level->adventureSettings; as.noMvP = !as.noMvP; AdventureSettingsPacket p(as); mc->raknetInstance->send(p); break; }
        case ACT_IMMUTABLE:          { auto& as = mc->level->adventureSettings; as.immutableWorld = !as.immutableWorld; AdventureSettingsPacket p(as); mc->raknetInstance->send(p); break; }
        case ACT_NAMETAGS:           { auto& as = mc->level->adventureSettings; as.showNameTags = !as.showNameTags; AdventureSettingsPacket p(as); mc->raknetInstance->send(p); break; }
        case ACT_PARTICLES: {
            Level* lvl = mc->level;
            if (!lvl) return;
            float px = mc->player->x, py = mc->player->y, pz = mc->player->z;
            for (int i = 0; i < 50; ++i) {
                lvl->addParticle("explode", px, py + 1.0f, pz,
                                0.02f * (rand() % 100 - 50), 0.02f * (rand() % 100),
                                0.02f * (rand() % 100 - 50));
                lvl->addParticle("largesmoke", px, py + 1.0f, pz,
                                0.04f * (rand() % 100 - 50), 0.04f * (rand() % 100),
                                0.04f * (rand() % 100 - 50));
            }
            break;
        }
    }
}
