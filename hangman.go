// hangman.go
package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"
	"strings"
	"time"
)

const (
	reset  = "\033[0m"
	green  = "\033[92m"
	red    = "\033[91m"
	yellow = "\033[93m"
	blue   = "\033[94m"
	cyan   = "\033[96m"
	bold   = "\033[1m"
)

func colorize(text, color string) string {
	return color + text + reset
}

var wordsMap = map[string][]string{
	"animals":     {"кот", "собака", "тигр", "слон", "жираф", "дельфин", "медведь", "волк", "лиса", "заяц"},
	"cities":      {"москва", "париж", "лондон", "берлин", "рим", "токио", "пекин", "нью-йорк", "каир", "мехико"},
	"food":        {"пицца", "суши", "бургер", "паста", "салат", "суп", "стейк", "омлет", "торт", "мороженое"},
	"professions": {"врач", "учитель", "инженер", "пилот", "повар", "художник", "музыкант", "архитектор", "юрист", "журналист"},
	"general":     {"программирование", "алгоритм", "компьютер", "интернет", "игра", "код", "данные", "сервер", "сайт", "приложение"},
}

var hangmanPics = []string{
	`
      +---+
          |
          |
          |
          |
          |
    =========
    `,
	`
      +---+
      |   |
          |
          |
          |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
          |
          |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
      |   |
          |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
     /|   |
          |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
     /|\  |
          |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
     /|\  |
     /    |
          |
    =========
    `,
	`
      +---+
      |   |
      O   |
     /|\  |
     / \  |
          |
    =========
    `,
}

type Stats struct {
	Games     int `json:"games"`
	Wins      int `json:"wins"`
	Losses    int `json:"losses"`
	Streak    int `json:"streak"`
	BestStreak int `json:"best_streak"`
}

type Hangman struct {
	maxErrors int
	theme     string
	word      string
	display   []rune
	guessed   map[rune]bool
	wrong     map[rune]bool
	errors    int
	hintUsed  bool
	gameOver  bool
	won       bool
	stats     Stats
	statsFile string
}

func NewHangman(maxErrors int, theme string) *Hangman {
	h := &Hangman{
		maxErrors: maxErrors,
		theme:     theme,
		guessed:   make(map[rune]bool),
		wrong:     make(map[rune]bool),
		statsFile: filepath.Join(os.Getenv("HOME"), ".hangman_stats.json"),
	}
	h.loadStats()
	words := wordsMap[theme]
	if len(words) == 0 {
		words = wordsMap["general"]
	}
	rand.Seed(time.Now().UnixNano())
	h.word = words[rand.Intn(len(words))]
	h.display = make([]rune, len(h.word))
	for i := range h.display {
		h.display[i] = '_'
	}
	return h
}

func (h *Hangman) loadStats() {
	data, err := os.ReadFile(h.statsFile)
	if err != nil {
		h.stats = Stats{}
		return
	}
	json.Unmarshal(data, &h.stats)
}

func (h *Hangman) saveStats() {
	data, _ := json.MarshalIndent(h.stats, "", "  ")
	os.WriteFile(h.statsFile, data, 0644)
}

func (h *Hangman) displayState() {
	fmt.Print("\033[H\033[2J")
	fmt.Println(colorize("🎯 ВИСЕЛИЦА", bold))
	fmt.Println(colorize("Тема: "+h.theme, blue))
	col := green
	if h.errors > 0 {
		col = red
	}
	fmt.Printf("%s\n", colorize(fmt.Sprintf("Ошибок: %d/%d", h.errors, h.maxErrors), col))
	fmt.Println(hangmanPics[h.errors])
	fmt.Print("Слово: ")
	for _, ch := range h.display {
		fmt.Printf("%c ", ch)
	}
	fmt.Println()
	if len(h.guessed) > 0 {
		fmt.Print(colorize("Открытые буквы: ", green))
		for ch := range h.guessed {
			fmt.Printf("%c ", ch)
		}
		fmt.Println()
	}
	if len(h.wrong) > 0 {
		fmt.Print(colorize("Ошибочные: ", red))
		for ch := range h.wrong {
			fmt.Printf("%c ", ch)
		}
		fmt.Println()
	}
}

func (h *Hangman) guess(letter rune) bool {
	letter = unicode.ToLower(letter)
	if h.guessed[letter] || h.wrong[letter] {
		fmt.Println(colorize("Вы уже называли эту букву.", yellow))
		return false
	}
	if strings.ContainsRune(h.word, letter) {
		h.guessed[letter] = true
		for i, ch := range h.word {
			if ch == letter {
				h.display[i] = letter
			}
		}
		if !strings.ContainsRune(string(h.display), '_') {
			h.won = true
			h.gameOver = true
		}
		return true
	} else {
		h.wrong[letter] = true
		h.errors++
		if h.errors >= h.maxErrors {
			h.gameOver = true
		}
		return false
	}
}

func (h *Hangman) hint() {
	if h.hintUsed {
		fmt.Println(colorize("Подсказка уже использована.", yellow))
		return
	}
	for i, ch := range h.display {
		if ch == '_' {
			h.display[i] = rune(h.word[i])
			h.guessed[rune(h.word[i])] = true
			h.hintUsed = true
			fmt.Printf("%s\n", colorize(fmt.Sprintf("Подсказка: буква '%c' открыта.", h.word[i]), cyan))
			if !strings.ContainsRune(string(h.display), '_') {
				h.won = true
				h.gameOver = true
			}
			break
		}
	}
}

func (h *Hangman) play() {
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println(colorize("Добро пожаловать в Виселицу!", bold))
	fmt.Println("Вводите буквы. ? - подсказка. quit - выход.")
	for !h.gameOver {
		h.displayState()
		fmt.Print("> ")
		scanner.Scan()
		cmd := strings.TrimSpace(scanner.Text())
		if cmd == "quit" {
			fmt.Println("Выход.")
			return
		}
		if cmd == "?" {
			h.hint()
			continue
		}
		if len(cmd) != 1 || !unicode.IsLetter(rune(cmd[0])) {
			fmt.Println(colorize("Введите одну букву.", red))
			continue
		}
		h.guess(rune(cmd[0]))
	}
	h.displayState()
	if h.won {
		fmt.Println(colorize("🎉 Поздравляем! Вы выиграли!", green))
		h.stats.Wins++
		h.stats.Streak++
		if h.stats.Streak > h.stats.BestStreak {
			h.stats.BestStreak = h.stats.Streak
		}
	} else {
		fmt.Printf("%s\n", colorize(fmt.Sprintf("💀 Вы проиграли. Загаданное слово: %s", h.word), red))
		h.stats.Losses++
		h.stats.Streak = 0
	}
	h.stats.Games++
	h.saveStats()
	fmt.Printf("%s\n", colorize(fmt.Sprintf("Статистика: игр %d, побед %d, поражений %d, серия %d, лучшая %d",
		h.stats.Games, h.stats.Wins, h.stats.Losses, h.stats.Streak, h.stats.BestStreak), blue))
}

func main() {
	maxErrors := 10
	theme := "general"
	showStats := false
	resetStats := false
	args := os.Args[1:]
	for i := 0; i < len(args); i++ {
		arg := args[i]
		switch arg {
		case "easy":
			maxErrors = 10
		case "medium":
			maxErrors = 7
		case "hard":
			maxErrors = 5
		case "-t", "--theme":
			if i+1 < len(args) {
				theme = args[i+1]
				if _, ok := wordsMap[theme]; !ok {
					theme = "general"
				}
				i++
			}
		case "-s", "--stats":
			showStats = true
		case "-r", "--reset":
			resetStats = true
		case "-h", "--help":
			fmt.Println("Usage: hangman [easy|medium|hard] [-t theme] [-s] [-r]")
			return
		}
	}
	if resetStats {
		f := filepath.Join(os.Getenv("HOME"), ".hangman_stats.json")
		os.Remove(f)
		fmt.Println("Статистика сброшена.")
		return
	}
	if showStats {
		f := filepath.Join(os.Getenv("HOME"), ".hangman_stats.json")
		data, err := os.ReadFile(f)
		if err != nil {
			fmt.Println("Статистика пуста.")
			return
		}
		var stats Stats
		json.Unmarshal(data, &stats)
		fmt.Println(colorize("📊 Статистика:", bold))
		fmt.Printf("  Игр: %d\n", stats.Games)
		fmt.Printf("  Побед: %d\n", stats.Wins)
		fmt.Printf("  Поражений: %d\n", stats.Losses)
		fmt.Printf("  Текущая серия: %d\n", stats.Streak)
		fmt.Printf("  Лучшая серия: %d\n", stats.BestStreak)
		return
	}
	game := NewHangman(maxErrors, theme)
	game.play()
}
