package main

import (
	"bufio"
	"fmt"
	"math/rand"
	"os"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"
	"unsafe"
)

// Constants for GetSaveFileName function
const (
	OFN_PATHMUSTEXIST   = 0x00000800
	OFN_FILEMUSTEXIST   = 0x00001000
	OFN_NOCHANGEDIR     = 0x00000008
	OFN_OVERWRITEPROMPT = 0x00000002
)

// GameResult stores the result of a single game
type GameResult struct {
	Winner    string
	AMoves    int
	BMoves    int
}

// playGame simulates a single game
func playGame(rng *rand.Rand) GameResult {
	deck := rng.Perm(52)
	for i := range deck {
		deck[i] = deck[i]%13 + 1
	}

	aDeck := deck[:26]
	bDeck := deck[26:]
	var table []int
	aTurn := true
	aMoves := 0
	bMoves := 0

	for len(aDeck) > 0 && len(bDeck) > 0 {
		var card int
		if aTurn {
			card = aDeck[0]
			aDeck = aDeck[1:]
			aMoves++
		} else {
			card = bDeck[0]
			bDeck = bDeck[1:]
			bMoves++
		}

		table = append(table, card)
		aTurn = !aTurn

		for i := 0; i < len(table)-1; i++ {
			if table[i] == card {
				if aTurn {
					aDeck = append(aDeck, table[i:]...)
				} else {
					bDeck = append(bDeck, table[i:]...)
				}
				table = table[:i]
				break
			}
		}
	}

	winner := "A"
	if len(aDeck) == 0 {
		winner = "B"
	}

	return GameResult{Winner: winner, AMoves: aMoves, BMoves: bMoves}
}

// simulateGames simulates a number of games in parallel
func simulateGames(numGames, numWorkers int) (int, int, []int, []int) {
	var wg sync.WaitGroup
	gameResults := make(chan GameResult, numGames)
	progress := make(chan int, numGames)

	rngPool := sync.Pool{
		New: func() interface{} {
			return rand.New(rand.NewSource(time.Now().UnixNano()))
		},
	}

	for i := 0; i < numWorkers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			localRng := rngPool.Get().(*rand.Rand)
			defer rngPool.Put(localRng)

			for j := 0; j < numGames/numWorkers; j++ {
				result := playGame(localRng)
				gameResults <- result
				progress <- 1
			}
		}()
	}

	go func() {
		wg.Wait()
		close(gameResults)
		close(progress)
	}()

	progressStep := determineProgressStep(numGames)
	totalProgress := 0
	startTime := time.Now()

	for p := range progress {
		totalProgress += p
		if totalProgress%progressStep == 0 {
			elapsed := time.Since(startTime).Seconds()
			estimatedTotalTime := elapsed / (float64(totalProgress) / float64(numGames))
			remainingTime := estimatedTotalTime - elapsed
			fmt.Printf("\r进度: %.4f%% - 剩余时间: %.2f秒", float64(totalProgress)/float64(numGames)*100, remainingTime)
		}
	}
	fmt.Println("\n模拟完成")

	aWins := 0
	bWins := 0
	aMovesList := make([]int, 0, numGames)
	bMovesList := make([]int, 0, numGames)

	for result := range gameResults {
		if result.Winner == "A" {
			aWins++
		} else {
			bWins++
		}
		aMovesList = append(aMovesList, result.AMoves)
		bMovesList = append(bMovesList, result.BMoves)
	}

	return aWins, bWins, aMovesList, bMovesList
}

// determineProgressStep determines the progress update step based on the number of games
func determineProgressStep(numGames int) int {
	switch {
	case numGames > 10000000000:
		return numGames / 100000
	case numGames > 1000000000:
		return numGames / 10000
	case numGames > 100000000:
		return numGames / 1000
	default:
		return numGames / 1000
	}
}

// saveToFile saves the slice of integers to a file
func saveToFile(filename string, data []int) error {
	file, err := os.Create(filename)
	if err != nil {
		return err
	}
	defer file.Close()

	writer := bufio.NewWriter(file)
	for _, value := range data {
		fmt.Fprintln(writer, value)
	}
	return writer.Flush()
}

// getSaveFileName opens a file dialog for saving a file and returns the selected file path
func getSaveFileName() (string, error) {
	var ofn struct {
		lStructSize       uint32
		hwndOwner         uintptr
		hInstance         uintptr
		lpstrFilter       *uint16
		lpstrCustomFilter *uint16
		nMaxCustFilter    uint32
		nFilterIndex      uint32
		lpstrFile         *uint16
		nMaxFile          uint32
		lpstrFileTitle    *uint16
		nMaxFileTitle     uint32
		lpstrInitialDir   *uint16
		lpstrTitle        *uint16
		Flags             uint32
		nFileOffset       uint16
		nFileExtension    uint16
		lpstrDefExt       *uint16
		lCustData         uintptr
		lpfnHook          uintptr
		lpTemplateName    *uint16
	}

	var szFile [260]uint16
	ofn.lStructSize = uint32(unsafe.Sizeof(ofn))
	ofn.lpstrFile = &szFile[0]
	ofn.nMaxFile = uint32(len(szFile))
	ofn.Flags = OFN_PATHMUSTEXIST | OFN_FILEMUSTEXIST | OFN_OVERWRITEPROMPT

	r1, _, err := syscall.Syscall6(
		procGetSaveFileName.Addr(),
		1,
		uintptr(unsafe.Pointer(&ofn)),
		0,
		0,
		0,
		0,
		0,
	)
	if r1 == 0 {
		return "", err
	}
	return syscall.UTF16ToString(szFile[:]), nil
}

var (
	libComdlg32        = syscall.NewLazyDLL("comdlg32.dll")
	procGetSaveFileName = libComdlg32.NewProc("GetSaveFileNameW")
)

func main() {
	rand.Seed(time.Now().UnixNano())

	reader := bufio.NewReader(os.Stdin)

	fmt.Print("输入游戏数量: ")
	numGamesStr, _ := reader.ReadString('\n')
	numGamesStr = strings.TrimSpace(numGamesStr)
	numGames, err := strconv.Atoi(numGamesStr)
	if err != nil {
		fmt.Println("无效的输入，请输入一个整数")
		return
	}

	fmt.Print("输入线程数量: ")
	numWorkersStr, _ := reader.ReadString('\n')
	numWorkersStr = strings.TrimSpace(numWorkersStr)
	numWorkers, err := strconv.Atoi(numWorkersStr)
	if err != nil {
		fmt.Println("无效的输入，请输入一个整数")
		return
	}

	fmt.Println("选择保存文件的路径")
	saveDir, err := getSaveFileName()
	if err != nil {
		fmt.Printf("无法选择文件路径: %v\n", err)
		return
	}

	fmt.Printf("Simulating %d games using %d threads\n", numGames, numWorkers)
	runtime.GOMAXPROCS(numWorkers)
	aWins, bWins, aMovesList, bMovesList := simulateGames(numGames, numWorkers)

	fmt.Printf("A赢了 %d 次\n", aWins)
	fmt.Printf("B赢了 %d 次\n", bWins)

	aMovesFile := saveDir + "_a_moves_list.txt"
	bMovesFile := saveDir + "_b_moves_list.txt"

	if err := saveToFile(aMovesFile, aMovesList); err != nil {
		fmt.Printf("Failed to save A's moves to file: %v\n", err)
	}
	if err := saveToFile(bMovesFile, bMovesList); err != nil {
		fmt.Printf("Failed to save B's moves to file: %v\n", err)
	}

	fmt.Printf("A每局游戏出牌次数已保存到 %s\n", aMovesFile)
	fmt.Printf("B每局游戏出牌次数已保存到 %s\n", bMovesFile)

	fmt.Println("按回车键退出")
	reader.ReadString('\n')
}