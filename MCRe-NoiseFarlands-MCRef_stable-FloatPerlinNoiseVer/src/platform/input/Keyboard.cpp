#include "Keyboard.h"

int Keyboard::_states[256] = {0};

std::vector<KeyboardAction> Keyboard::_inputs;
std::vector<char> Keyboard::_inputText;

int	Keyboard::_index = -1;

int	Keyboard::_textIndex = -1;

const char* Keyboard::getKeyName(int key) {
	switch (key) {
		case KEY_A: return "A";
		case KEY_B: return "B";
		case KEY_C: return "C";
		case KEY_D: return "D";
		case KEY_E: return "E";
		case KEY_F: return "F";
		case KEY_G: return "G";
		case KEY_H: return "H";
		case KEY_I: return "I";
		case KEY_J: return "J";
		case KEY_K: return "K";
		case KEY_L: return "L";
		case KEY_M: return "M";
		case KEY_N: return "N";
		case KEY_O: return "O";
		case KEY_P: return "P";
		case KEY_Q: return "Q";
		case KEY_R: return "R";
		case KEY_S: return "S";
		case KEY_T: return "T";
		case KEY_U: return "U";
		case KEY_V: return "V";
		case KEY_W: return "W";
		case KEY_X: return "X";
		case KEY_Y: return "Y";
		case KEY_Z: return "Z";
		case KEY_BACKSPACE: return "Backspace";
		case KEY_RETURN: return "Return";
		case KEY_F1: return "F1";
		case KEY_F2: return "F2";
		case KEY_F3: return "F3";
		case KEY_F4: return "F4";
		case KEY_F5: return "F5";
		case KEY_F6: return "F6";
		case KEY_F7: return "F7";
		case KEY_F8: return "F8";
		case KEY_F9: return "F9";
		case KEY_F10: return "F10";
		case KEY_F11: return "F11";
		case KEY_F12: return "F12";
		case KEY_ESCAPE: return "Esc";
		case KEY_SPACE: return "Space";
		case KEY_LSHIFT: return "Left Shift";
		default: return "Unknown";
	}
}