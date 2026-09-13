#ifndef NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchInputHolder_H__
#define NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchInputHolder_H__

#include "../IInputHolder.h"
#include "TouchscreenInput.h"

#include "../ITurnInput.h"
#include "TouchAreaModel.h"
#include "../../../../platform/input/Multitouch.h"
#include "../../../../platform/time.h"
#include "../../../../util/SmoothFloat.h"

#include "../../../../world/entity/player/Player.h"
#include "../../../../world/entity/player/Inventory.h"

template <class T>
class ModifyNotify {
public:
	ModifyNotify()
	:	_changed(false)
	{}

	T& getOld() { return _old; }
	T& getNew() { return _new; }

	bool update(T& val) {
		_changed = !equals(val);
		if (_changed) {
			_old = _new;
			_new = val;
		}
		return _changed;
	}

	bool hasChanged() const { return _changed; }

	virtual bool equals(const T& newVal) {
		return _old != newVal;
	}
protected:
	T _old;
private:
	T _new;
	bool _changed;
};

//
// Implementation for unified [Turn & Build Input]
//
class UnifiedTurnBuild: public GuiComponent,
						public ITurnInput,
						public IBuildInput
{
public:
	static const int MODE_OFFSET = 1;
	static const int MODE_DELTA  = 2;

	UnifiedTurnBuild(int turnMode, int width, int height, float maxMovementDelta, float sensitivity, IInputHolder* holder, Minecraft* minecraft)
	:	mode(turnMode),
		_holder(holder),
		_options(&minecraft->options),
		cxO(0), cyO(0),
		wasActive(false),
		_totalMoveDelta(0),
		_maxMovement(maxMovementDelta),
		_lastPlayer(0),
		screenArea(-1, -1, 0, 0),
		allowPicking(false),
		state(State_None),
		moveArea(-1,-1,0,0),
		joyTouchArea(-1, -1, 0, 0),
		inventoryArea(-1,-1, 0, 0),
		pauseArea(-1, -1, 0, 0),
		_buildMovement(0),
		_sentFirstRemove(false),
		_canDestroy(false),
		_forceCanUse(false)
	{
		_area.deleteMe = false;
		setSensitivity(sensitivity);
		onConfigChanged(createConfig(minecraft));

		_lastBuildDownTime = _startTurnTime = getTimeS();
	}

	void setSensitivity(float sensitivity) {
		_sensitivity = sensitivity;
	}

	void addExcludeArea(IArea* area) {
		_area.exclude(area);
	}

	virtual void onConfigChanged(const Config& c) override {
		// 注意：原版代码中是通过 Options::isJoyTouchArea 访问，但 Options 没有该成员。
		// 改为调用 getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA)
		bool isJoyTouchArea = _options->getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA);
		if (false && isJoyTouchArea) {  // 原本就是 false && ...，忽略该分支
			// ... 原 joyTouchArea 逻辑（实际从未执行）
		} else {
			screenArea = RectangleArea(0, 0, (float)c.width, (float)c.height);
			// Expand the move area a bit
            const float border = 10;
			const float widen = (moveArea._x1-moveArea._x0) * 0.05f + border; // ~5% wider
			moveArea._x0 -= widen;
			moveArea._x1 += widen;
			const float heighten = (moveArea._y1-moveArea._y0) * 0.05f + border; // ~5% taller
			moveArea._y0 -= heighten;
			moveArea._y1 += heighten;
            
            pauseArea._x0 -= border;
            pauseArea._x1 += border;
            pauseArea._y0 -= border;
            pauseArea._y1 += border;

			_area.clear();
			_area.include(&screenArea);
			_area.exclude(&moveArea);
			_area.exclude(&inventoryArea);
#ifdef __APPLE__
            _area.exclude(&pauseArea);
#endif
			_model.clear();
			_model.addArea(AREA_TURN, &_area);
		}
	}

	float calcNewAlpha(float current, float wanted) {
		if (wanted > current)
			return Mth::clamp(current + 0.02f, 0.0f, wanted);
		if (wanted < current)
			return Mth::clamp(current - 0.04f, wanted, 1.0f);
		return current;
	}

	//
	// Implementation for the ITurnInput part
	//
	TurnDelta getTurnDelta() override {
		float dx = 0, dy = 0;
		const float now = getTimeS();

		float cx = 0;
		float cy = 0;
		bool isActive = false;

		const int* pointerIds;
		bool wasFirstMovement = false;
		int pointerCount = Multitouch::getActivePointerIds(&pointerIds);
		for (int i = 0; i < pointerCount; ++i) {
			int p = pointerIds[i];
			int x = Multitouch::getX(p);
			int y = Multitouch::getY(p);
			int areaId = _model.getPointerId(x, y, p);

			if (areaId == AREA_TURN) {
				isActive = true;
				cx = (float)x * 0.5f;
				cy = (float)y * -0.5f;
				wasFirstMovement = Multitouch::wasFirstMovement(p);
				break;
			}
		}
		
		if(isActive && !wasActive){
        _startTurnTime = now;
        _totalMoveDelta = 0;
        bool isInMovement = _lastPlayer ? getSpeedSquared(_lastPlayer) > 0.01f : false;
        state = State_Deciding;
        _canDestroy = !isInMovement;
        
        // 🧊 修复：每次新触摸都重置 _forceCanUse
        // 不能让它从一个触摸周期"泄漏"到下一个
        _forceCanUse = false;
        
        if(!_canDestroy && (_lastPlayer && _lastPlayer->canUseCarriedItemWhileMoving())){
            _forceCanUse = true;
            _canDestroy = true;
        }
        _sentFirstRemove = false;
    } else if(wasActive && !isActive){
        _sentFirstRemove = false;
        state = State_None;
        // 🧊 手指抬起时也重置，双保险
        _canDestroy = false;
        _forceCanUse = false;
		}

		if (MODE_DELTA == mode && (wasActive || isActive)) {
			const float DeadZone = 0;

			if (!wasActive) {
				cxO = cx;
				cyO = cy;
			}
			if (isActive) {
				dx = _sensitivity * linearTransform(cx - cxO, DeadZone);
				dy = _sensitivity * linearTransform(cy - cyO, DeadZone);
				
				float moveDelta = ( Mth::abs(dx) + Mth::abs(dy) );

				if (moveDelta > _maxMovement) {
					dx = 0;
					dy = 0;
					moveDelta = 0;
				}
				_totalMoveDelta += moveDelta;

				// 状态机：先判断移动是否超过阈值，再判断长按时间
				if (state == State_Deciding) {
					const float since = now - _startTurnTime;
					if (_totalMoveDelta > MaxBuildMovement) {
						state = State_Turn;
					} else if (since >= (RemovalMilliseconds / 1000.0f)) {
						state = State_Destroy;
						_canDestroy = true;
					}
				}

				if (wasFirstMovement) {
					dx = dy = 0;
				}

				cxO = cx;
				cyO = cy;
			}
		} else {
            state = State_None;
        }

		updateFeedbackProgressAlpha(now);

		wasActive = isActive;
		return TurnDelta(dx, -dy);
	}

	void updateFeedbackProgressAlpha(float now) {
		const float MinAlphaValue = -0.05f;
		if (_canDestroy) {
			const float since = now - _startTurnTime;
			if (state == State_Deciding) {
				const float wantedAlpha = since / (0.001f*RemovalMilliseconds);
				_holder->alpha = wantedAlpha * wantedAlpha;
			} else {
				if (state == State_Destroy) {
					_holder->alpha = calcNewAlpha(_holder->alpha, 1);
				} else if (state == State_Turn || state == State_None) {
					_holder->alpha = calcNewAlpha(_holder->alpha, 0);
				}
			}
		} else {
			_holder->alpha = MinAlphaValue;
		}
	}

	bool isInsideArea(float x, float y) {
		return _area.isInside(x, y);
	}

	int mode;

	static float getSpeedSquared(Entity* m) {
		const float xd = m->x - m->xo;
		const float yd = m->y - m->yo;
		const float zd = m->z - m->zo;
		const float speedSquared = xd*xd + yd*yd + zd*zd;
		return speedSquared;
	}

	void render(float alpha) {
		bool isJoyTouchArea = _options->getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA);
		if (isJoyTouchArea) {
			fill(	(int) (Gui::InvGuiScale * joyTouchArea._x0),
					(int) (Gui::InvGuiScale * joyTouchArea._y0),
					(int) (Gui::InvGuiScale * joyTouchArea._x1),
					(int) (Gui::InvGuiScale * joyTouchArea._y1), 0x40000000);
		}
	}

	//
	// Implementation for the IBuildInput part
	//
	virtual bool tickBuild(Player* player, BuildActionIntention* bai) override {
    _lastPlayer = player;

    if(state == State_Destroy){
        if(!_sentFirstRemove){
            // 🧊 修复：_forceCanUse 不应抑制 BAI_FIRSTREMOVE/BAI_REMOVE
            // 它只应该影响 BAI_INTERACT（是否允许"使用"物品）
            // 破坏方块始终应该允许
            *bai = BuildActionIntention(
                BuildActionIntention::BAI_FIRSTREMOVE
                | (_forceCanUse ? 0 : BuildActionIntention::BAI_INTERACT)  // 只在非forceCanUse时加交互
            );
            _sentFirstRemove = true;
        } else {
            *bai = BuildActionIntention(
                BuildActionIntention::BAI_REMOVE
                | (_forceCanUse ? 0 : BuildActionIntention::BAI_INTERACT)
            );
        }
        return true;
    }
    
    // ... 后面不变 ...

		Multitouch::rewind();
		const float now = getTimeS();
		allowPicking = false;
		bool handled = false;

		while (Multitouch::next()) {
			MouseAction& m = Multitouch::getEvent();
			if (m.action == MouseAction::ACTION_MOVE) continue;

			int areaId = _model.getPointerId(m.x, m.y, m.pointerId);
			if (areaId != AREA_TURN) continue;

			allowPicking = true;

			if (m.data == MouseAction::DATA_UP && !handled) {
				if (_totalMoveDelta <= MaxBuildMovement) {
					*bai = BuildActionIntention(BuildActionIntention::BAI_BUILD | BuildActionIntention::BAI_ATTACK);
					handled = true;
				}
				state = State_None;
				_sentFirstRemove = false;
				_canDestroy = false;
			} else if (m.data == MouseAction::DATA_DOWN) {
				_lastBuildDownTime = now;
				_buildMovement = 0;
				state = State_Deciding;
			}
		}
		return handled;
	}

	bool allowPicking;
	float alpha;
	SmoothFloat smoothAlpha;
	
	RectangleArea screenArea;
	RectangleArea moveArea;
	RectangleArea joyTouchArea;
	RectangleArea inventoryArea;
    RectangleArea pauseArea;

private:
	IInputHolder* _holder;

	// Turn
	int cid;
	float cxO, cyO;
	bool wasActive;

	TouchAreaModel _model;
	IncludeExcludeArea _area;

	bool _decidedTurnMode;

	float _startTurnTime;
	float _totalMoveDelta;
	float _maxMovement;
	float _sensitivity;

	Player* _lastPlayer;

	// Build
	float _lastBuildDownTime;
	float _buildMovement;
	bool _sentFirstRemove;
	bool _canDestroy;
	bool _forceCanUse;

	int state;
	static const int State_None = 0;
	static const int State_Deciding = 1;
	static const int State_Turn = 2;
	static const int State_Destroy = 3;
	static const int State_Build = 4; // Will never happen

	// 优化后的参数：原版手感
	static const int MaxBuildMovement = 6;
	static const int RemovalMilliseconds = 300;

	static const int AREA_TURN = 100;
	Options* _options;
};

class Minecraft;

class TouchInputHolder: public IInputHolder
{
public:
	TouchInputHolder(Minecraft* mc, Options* options)
	:	_mc(mc),
		_move(mc, options),
		_turnBuild(UnifiedTurnBuild::MODE_DELTA, mc->width, mc->height, (float)MovementLimit, 1, this, mc),
		_jumpExclude(0,0,0,0),     // 临时初始化，实际值在 onConfigChanged 中重新赋值
		_flyUpExclude(0,0,0,0),
		_flyDownExclude(0,0,0,0),
		_chatExclude(0,0,0,0),
		_pauseExclude(0,0,0,0)
	{
		onConfigChanged(createConfig(mc));
	}
	~TouchInputHolder() {
	}

	virtual void onConfigChanged(const Config& c) override {
		_move.onConfigChanged(c);
		_turnBuild.moveArea = _move.getRectangleArea();
#ifdef __APPLE__
		_turnBuild.pauseArea = _move.getPauseRectangleArea();
#endif
		bool isLeftHanded = _mc->options.getBooleanValue(OPTIONS_IS_LEFT_HANDED);
		_turnBuild.inventoryArea = _mc->gui.getRectangleArea(isLeftHanded ? 1 : -1);

		// 更新排除区域（需重新赋值，因为矩形区域可能因屏幕尺寸变化而改变）
		_jumpExclude = _move.getJumpRectangleArea();
		_flyUpExclude = _move.getFlyUpRectangleArea();
		_flyDownExclude = _move.getFlyDownRectangleArea();
		_chatExclude = _move.getChatRectangleArea();
		_pauseExclude = _move.getPauseRectangleArea();

		_turnBuild.addExcludeArea(&_jumpExclude);
		_turnBuild.addExcludeArea(&_flyUpExclude);
		_turnBuild.addExcludeArea(&_flyDownExclude);
		_turnBuild.addExcludeArea(&_chatExclude);
		_turnBuild.addExcludeArea(&_pauseExclude);

		bool isJoyTouchArea = c.options->getBooleanValue(OPTIONS_IS_JOY_TOUCH_AREA);
		_turnBuild.setSensitivity(isJoyTouchArea ? 1.8f : 1.0f);
		((ITurnInput*)&_turnBuild)->onConfigChanged(c);
	}

	virtual bool allowPicking() override {
		const int* pointerIds;
		int pointerCount = Multitouch::getActivePointerIds(&pointerIds);
		for (int i = 0; i < pointerCount; ++i) {
			int p = pointerIds[i];
			const float x = Multitouch::getX(p);
			const float y = Multitouch::getY(p);

			if (_turnBuild.isInsideArea(x, y)) {
				mousex = x;
				mousey = y;
				return true;
			}
		}
		return false;
	}

	virtual void render(float alpha) override {
		_turnBuild.render(alpha);
	}

	virtual IMoveInput*		getMoveInput()  override { return &_move; }
	virtual ITurnInput*		getTurnInput()  override { return &_turnBuild; }
	virtual IBuildInput*	getBuildInput() override { return &_turnBuild; }

private:
	TouchscreenInput_TestFps _move;
	UnifiedTurnBuild _turnBuild;
	Minecraft* _mc;

	// 存储排除区域的副本，保证指针生命周期
	RectangleArea _jumpExclude;
	RectangleArea _flyUpExclude;
	RectangleArea _flyDownExclude;
	RectangleArea _chatExclude;
	RectangleArea _pauseExclude;

	static const int MovementLimit = 200;
};

#endif /*NET_MINECRAFT_CLIENT_PLAYER_INPUT_TOUCHSCREEN_TouchInputHolder_H__*/
