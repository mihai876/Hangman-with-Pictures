// hangman.cpp
#include <iostream>
#include <vector>
#include <string>
#include <map>
#include <random>
#include <algorithm>
#include <fstream>
#include <cctype>
#include <filesystem>

using namespace std;
namespace fs = std::filesystem;

const string RESET = "\033[0m";
const string GREEN = "\033[92m";
const string RED = "\033[91m";
const string YELLOW = "\033[93m";
const string BLUE = "\033[94m";
const string BOLD = "\033[1m";

string colorize(const string& text, const string& color) {
    return color + text + RESET;
}

string getHomeDir() {
    const char* home = getenv("HOME");
    if (!home) home = getenv("USERPROFILE");
    return string(home);
}

map<string, vector<string>> WORDS = {
    {"animals", {"кот","собака","тигр","слон","жираф","дельфин","медведь","волк","лиса","заяц"}},
    {"cities", {"москва","париж","лондон","берлин","рим","токио","пекин","нью-йорк","каир","мехико"}},
    {"food", {"пицца","суши","бургер","паста","салат","суп","стейк","омлет","торт","мороженое"}},
    {"professions", {"врач","учитель","инженер","пилот","повар","художник","музыкант","архитектор","юрист","журналист"}},
    {"general", {"программирование","алгоритм","компьютер","интернет","игра","код","данные","сервер","сайт","приложение"}}
};

vector<string> HANGMAN_PICS = {
    R"(
      +---+
          |
          |
          |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
          |
          |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
          |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
      |   |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
     /|   |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
     /|\\  |
          |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
     /|\\  |
     /    |
          |
    =========
    )",
    R"(
      +---+
      |   |
      O   |
     /|\\  |
     / \\  |
          |
    =========
    )"
};

class Hangman {
public:
    int maxErrors;
    string theme;
    string word;
    vector<char> display;
    set<char> guessed;
    set<char> wrong;
    int errors;
    bool hintUsed;
    bool gameOver;
    bool won;
    string statsFile;
    map<string, int> stats;

    Hangman(int maxErr = 10, string th = "general") : maxErrors(maxErr), theme(th), errors(0), hintUsed(false), gameOver(false), won(false) {
        statsFile = getHomeDir() + "/.hangman_stats.json";
        loadStats();
        auto& words = WORDS[theme];
        if (words.empty()) words = WORDS["general"];
        random_device rd;
        mt19937 gen(rd());
        uniform_int_distribution<> dis(0, words.size()-1);
        word = words[dis(gen)];
        display.assign(word.size(), '_');
    }

    void loadStats() {
        ifstream f(statsFile);
        if (!f) {
            stats["games"] = 0; stats["wins"] = 0; stats["losses"] = 0; stats["streak"] = 0; stats["best_streak"] = 0;
            return;
        }
        string content((istreambuf_iterator<char>(f)), istreambuf_iterator<char>());
        // Упрощённый парсинг JSON
        auto extract = [&](const string& key) -> int {
            size_t pos = content.find("\"" + key + "\"");
            if (pos == string::npos) return 0;
            pos = content.find(":", pos) + 1;
            size_t end = content.find(",", pos);
            if (end == string::npos) end = content.find("}", pos);
            try { return stoi(content.substr(pos, end-pos)); } catch (...) { return 0; }
        };
        stats["games"] = extract("games");
        stats["wins"] = extract("wins");
        stats["losses"] = extract("losses");
        stats["streak"] = extract("streak");
        stats["best_streak"] = extract("best_streak");
    }

    void saveStats() {
        ofstream f(statsFile);
        if (f) {
            f << "{\"games\":" << stats["games"] << ",\"wins\":" << stats["wins"]
              << ",\"losses\":" << stats["losses"] << ",\"streak\":" << stats["streak"]
              << ",\"best_streak\":" << stats["best_streak"] << "}";
        }
    }

    void displayState() {
        cout << "\033[2J\033[1;1H";
        cout << colorize("🎯 ВИСЕЛИЦА", BOLD) << endl;
        cout << colorize("Тема: " + theme, BLUE) << endl;
        cout << colorize("Ошибок: " + to_string(errors) + "/" + to_string(maxErrors), (errors>0?RED:GREEN)) << endl;
        cout << HANGMAN_PICS[errors] << endl;
        cout << "Слово: ";
        for (char c : display) cout << c << " ";
        cout << endl;
        if (!guessed.empty()) {
            cout << colorize("Открытые буквы: ", GREEN);
            for (char c : guessed) cout << c << " ";
            cout << endl;
        }
        if (!wrong.empty()) {
            cout << colorize("Ошибочные: ", RED);
            for (char c : wrong) cout << c << " ";
            cout << endl;
        }
    }

    bool guess(char letter) {
        letter = tolower(letter);
        if (guessed.find(letter) != guessed.end() || wrong.find(letter) != wrong.end()) {
            cout << colorize("Вы уже называли эту букву.", YELLOW) << endl;
            return false;
        }
        if (word.find(letter) != string::npos) {
            guessed.insert(letter);
            for (size_t i=0; i<word.size(); ++i)
                if (word[i] == letter) display[i] = letter;
            if (find(display.begin(), display.end(), '_') == display.end()) {
                won = true;
                gameOver = true;
            }
            return true;
        } else {
            wrong.insert(letter);
            errors++;
            if (errors >= maxErrors) gameOver = true;
            return false;
        }
    }

    void hint() {
        if (hintUsed) {
            cout << colorize("Подсказка уже использована.", YELLOW) << endl;
            return;
        }
        for (size_t i=0; i<word.size(); ++i) {
            if (display[i] == '_') {
                display[i] = word[i];
                guessed.insert(word[i]);
                hintUsed = true;
                cout << colorize("Подсказка: буква '" + string(1, word[i]) + "' открыта.", CYAN) << endl;
                if (find(display.begin(), display.end(), '_') == display.end()) {
                    won = true;
                    gameOver = true;
                }
                break;
            }
        }
    }

    void play() {
        cout << colorize("Добро пожаловать в Виселицу!", BOLD) << endl;
        cout << "Вводите буквы. ? - подсказка. quit - выход." << endl;
        string cmd;
        while (!gameOver) {
            displayState();
            cout << "> ";
            getline(cin, cmd);
            if (cmd == "quit") { cout << "Выход." << endl; return; }
            if (cmd == "?") { hint(); continue; }
            if (cmd.size() != 1 || !isalpha(cmd[0])) {
                cout << colorize("Введите одну букву.", RED) << endl;
                continue;
            }
            guess(cmd[0]);
        }
        displayState();
        if (won) {
            cout << colorize("🎉 Поздравляем! Вы выиграли!", GREEN) << endl;
            stats["wins"]++;
            stats["streak"]++;
            if (stats["streak"] > stats["best_streak"]) stats["best_streak"] = stats["streak"];
        } else {
            cout << colorize("💀 Вы проиграли. Загаданное слово: " + word, RED) << endl;
            stats["losses"]++;
            stats["streak"] = 0;
        }
        stats["games"]++;
        saveStats();
        cout << colorize("Статистика: игр " + to_string(stats["games"]) + ", побед " + to_string(stats["wins"]) + ", поражений " + to_string(stats["losses"]) + ", серия " + to_string(stats["streak"]) + ", лучшая " + to_string(stats["best_streak"]), BLUE) << endl;
    }
};

int main(int argc, char* argv[]) {
    int maxErrors = 10;
    string theme = "general";
    bool showStats = false, resetStats = false;
    for (int i=1; i<argc; ++i) {
        string arg = argv[i];
        if (arg == "easy") maxErrors = 10;
        else if (arg == "medium") maxErrors = 7;
        else if (arg == "hard") maxErrors = 5;
        else if (arg == "-t" || arg == "--theme") {
            if (i+1 < argc) {
                theme = argv[++i];
                if (WORDS.find(theme) == WORDS.end()) theme = "general";
            }
        } else if (arg == "-s" || arg == "--stats") showStats = true;
        else if (arg == "-r" || arg == "--reset") resetStats = true;
        else if (arg == "-h" || arg == "--help") {
            cout << "Usage: hangman [easy|medium|hard] [-t theme] [-s] [-r]" << endl;
            return 0;
        }
    }
    if (resetStats) {
        string f = getHomeDir() + "/.hangman_stats.json";
        if (fs::exists(f)) fs::remove(f);
        cout << "Статистика сброшена." << endl;
        return 0;
    }
    if (showStats) {
        string f = getHomeDir() + "/.hangman_stats.json";
        ifstream file(f);
        if (file) {
            string content((istreambuf_iterator<char>(file)), istreambuf_iterator<char>());
            auto extract = [&](const string& key) -> int {
                size_t pos = content.find("\"" + key + "\"");
                if (pos == string::npos) return 0;
                pos = content.find(":", pos) + 1;
                size_t end = content.find(",", pos);
                if (end == string::npos) end = content.find("}", pos);
                try { return stoi(content.substr(pos, end-pos)); } catch (...) { return 0; }
            };
            int games = extract("games"), wins = extract("wins"), losses = extract("losses"), streak = extract("streak"), best = extract("best_streak");
            cout << colorize("📊 Статистика:", BOLD) << endl;
            cout << "  Игр: " << games << endl;
            cout << "  Побед: " << wins << endl;
            cout << "  Поражений: " << losses << endl;
            cout << "  Текущая серия: " << streak << endl;
            cout << "  Лучшая серия: " << best << endl;
        } else {
            cout << "Статистика пуста." << endl;
        }
        return 0;
    }
    Hangman game(maxErrors, theme);
    game.play();
    return 0;
}
