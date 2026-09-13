CXX ?= c++
CXXFLAGS ?= -std=c++11

TARGETS := fozu what

ifeq ($(OS),Windows_NT)
TARGETS += beep_win
endif

.PHONY: all clean

all: $(TARGETS)

fozu: fozu.cpp
	$(CXX) $(CXXFLAGS) $< -o $@

what: what.cpp
	$(CXX) $(CXXFLAGS) $< -o $@

beep_win: beep_win.cpp
	$(CXX) $(CXXFLAGS) $< -o $@

clean:
	rm -f $(TARGETS)