// hangman.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;

class Hangman
{
    static string Colorize(string text, string color)
    {
        string col = color switch
        {
            "green" => "\x1b[92m",
            "red" => "\x1b[91m",
            "yellow" => "\x1b[93m",
            "blue" => "\x1b[94m",
            "cyan" => "\x1b[96m",
            "bold" => "\x1b[1m",
            _ => "\x1b[0m"
        };
        return col + text + "\x1b[0m";
    }

    static Dictionary<string, List<string>> WORDS = new Dictionary<string, List<string>>
    {
        {"animals", new List<string>{"кот","собака","тигр","слон","жираф","дельфин","медведь","волк","лиса","заяц"}},
        {"cities", new List<string>{"москва","париж","лондон","берлин","рим","токио","пекин","нью-йорк","каир","мехико"}},
        {"food", new List<string>{"пицца","суши","бургер","паста","салат","суп","стейк","омлет","торт","мороженое"}},
        {"professions", new List<string>{"врач","учитель","инженер","пилот","повар","художник","музыкант","архитектор","юрист","журналист"}},
        {"general", new List<string>{"программирование","алгоритм","компьютер","интернет","игра","код","данные","сервер","сайт","приложение"}}
    };

    static string[] HANGMAN_PICS = {
        @"
      +---+
          |
          |
          |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
          |
          |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
          |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
      |   |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
     /|   |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
     /|\  |
          |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
     /|\  |
     /    |
          |
    =========
    ",
        @"
      +---+
      |   |
      O   |
     /|\  |
     / \  |
          |
    =========
    "
    };

    class Stats
    {
        public int games { get; set; }
        public int wins { get; set; }
        public int losses { get; set; }
        public int streak { get; set; }
        public int best_streak { get; set; }
    }

    private int maxErrors;
    private string theme;
    private string word;
    private char[] display;
    private HashSet<char> guessed = new HashSet<char>();
    private HashSet<char> wrong = new HashSet<char>();
    private int errors;
    private bool hintUsed;
    private bool gameOver;
    private bool won;
    private Stats stats;
    private string statsFile;

    public Hangman(int maxErr, string th)
    {
        maxErrors = maxErr;
        theme = th;
        statsFile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".hangman_stats.json");
        LoadStats();
        var words = WORDS.ContainsKey(theme) ? WORDS[theme] : WORDS["general"];
        Random rnd = new Random();
        word = words[rnd.Next(words.Count)];
        display = word.ToCharArray();
        for (int i = 0; i < display.Length; i++) display[i] = '_';
    }

    void LoadStats()
    {
        if (File.Exists(statsFile))
        {
            try
            {
                string json = File.ReadAllText(statsFile);
                stats = JsonSerializer.Deserialize<Stats>(json);
            }
            catch { stats = new Stats(); }
        }
        else stats = new Stats();
    }

    void SaveStats()
    {
        string json = JsonSerializer.Serialize(stats);
        File.WriteAllText(statsFile, json);
    }

    void DisplayState()
    {
        Console.Clear();
        Console.WriteLine(Colorize("🎯 ВИСЕЛИЦА", "bold"));
        Console.WriteLine(Colorize($"Тема: {theme}", "blue"));
        string col = errors > 0 ? "red" : "green";
        Console.WriteLine(Colorize($"Ошибок: {errors}/{maxErrors}", col));
        Console.WriteLine(HANGMAN_PICS[errors]);
        Console.Write("Слово: ");
        foreach (char c in display) Console.Write(c + " ");
        Console.WriteLine();
        if (guessed.Count > 0)
        {
            Console.Write(Colorize("Открытые буквы: ", "green"));
            Console.WriteLine(string.Join(" ", guessed));
        }
        if (wrong.Count > 0)
        {
            Console.Write(Colorize("Ошибочные: ", "red"));
            Console.WriteLine(string.Join(" ", wrong));
        }
    }

    bool Guess(char letter)
    {
        letter = char.ToLower(letter);
        if (guessed.Contains(letter) || wrong.Contains(letter))
        {
            Console.WriteLine(Colorize("Вы уже называли эту букву.", "yellow"));
            return false;
        }
        if (word.Contains(letter))
        {
            guessed.Add(letter);
            for (int i = 0; i < word.Length; i++)
                if (word[i] == letter) display[i] = letter;
            if (!display.Contains('_'))
            {
                won = true;
                gameOver = true;
            }
            return true;
        }
        else
        {
            wrong.Add(letter);
            errors++;
            if (errors >= maxErrors) gameOver = true;
            return false;
        }
    }

    void Hint()
    {
        if (hintUsed)
        {
            Console.WriteLine(Colorize("Подсказка уже использована.", "yellow"));
            return;
        }
        for (int i = 0; i < display.Length; i++)
        {
            if (display[i] == '_')
            {
                display[i] = word[i];
                guessed.Add(word[i]);
                hintUsed = true;
                Console.WriteLine(Colorize($"Подсказка: буква '{word[i]}' открыта.", "cyan"));
                if (!display.Contains('_'))
                {
                    won = true;
                    gameOver = true;
                }
                break;
            }
        }
    }

    public void Play()
    {
        Console.WriteLine(Colorize("Добро пожаловать в Виселицу!", "bold"));
        Console.WriteLine("Вводите буквы. ? - подсказка. quit - выход.");
        while (!gameOver)
        {
            DisplayState();
            Console.Write("> ");
            string cmd = Console.ReadLine().Trim();
            if (cmd == "quit") { Console.WriteLine("Выход."); return; }
            if (cmd == "?") { Hint(); continue; }
            if (cmd.Length != 1 || !char.IsLetter(cmd[0]))
            {
                Console.WriteLine(Colorize("Введите одну букву.", "red"));
                continue;
            }
            Guess(cmd[0]);
        }
        DisplayState();
        if (won)
        {
            Console.WriteLine(Colorize("🎉 Поздравляем! Вы выиграли!", "green"));
            stats.wins++;
            stats.streak++;
            if (stats.streak > stats.best_streak) stats.best_streak = stats.streak;
        }
        else
        {
            Console.WriteLine(Colorize($"💀 Вы проиграли. Загаданное слово: {word}", "red"));
            stats.losses++;
            stats.streak = 0;
        }
        stats.games++;
        SaveStats();
        Console.WriteLine(Colorize($"Статистика: игр {stats.games}, побед {stats.wins}, поражений {stats.losses}, серия {stats.streak}, лучшая {stats.best_streak}", "blue"));
    }

    static void Main(string[] args)
    {
        int maxErrors = 10;
        string theme = "general";
        bool showStats = false, resetStats = false;
        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            if (arg == "easy") maxErrors = 10;
            else if (arg == "medium") maxErrors = 7;
            else if (arg == "hard") maxErrors = 5;
            else if (arg == "-t" || arg == "--theme")
            {
                if (i + 1 < args.Length)
                {
                    theme = args[++i];
                    if (!WORDS.ContainsKey(theme)) theme = "general";
                }
            }
            else if (arg == "-s" || arg == "--stats") showStats = true;
            else if (arg == "-r" || arg == "--reset") resetStats = true;
            else if (arg == "-h" || arg == "--help")
            {
                Console.WriteLine("Usage: hangman [easy|medium|hard] [-t theme] [-s] [-r]");
                return;
            }
        }
        if (resetStats)
        {
            string f = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".hangman_stats.json");
            if (File.Exists(f)) File.Delete(f);
            Console.WriteLine("Статистика сброшена.");
            return;
        }
        if (showStats)
        {
            string f = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".hangman_stats.json");
            if (File.Exists(f))
            {
                try
                {
                    string json = File.ReadAllText(f);
                    var stats = JsonSerializer.Deserialize<Stats>(json);
                    Console.WriteLine(Colorize("📊 Статистика:", "bold"));
                    Console.WriteLine($"  Игр: {stats.games}");
                    Console.WriteLine($"  Побед: {stats.wins}");
                    Console.WriteLine($"  Поражений: {stats.losses}");
                    Console.WriteLine($"  Текущая серия: {stats.streak}");
                    Console.WriteLine($"  Лучшая серия: {stats.best_streak}");
                }
                catch { Console.WriteLine("Статистика пуста."); }
            }
            else Console.WriteLine("Статистика пуста.");
            return;
        }
        Hangman game = new Hangman(maxErrors, theme);
        game.Play();
    }
}
