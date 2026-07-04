// hangman.java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class hangman {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[93m";
    private static final String BLUE = "\u001B[94m";
    private static final String CYAN = "\u001B[96m";
    private static final String BOLD = "\u001B[1m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    private static final Map<String, List<String>> WORDS = new HashMap<>();
    static {
        WORDS.put("animals", Arrays.asList("кот","собака","тигр","слон","жираф","дельфин","медведь","волк","лиса","заяц"));
        WORDS.put("cities", Arrays.asList("москва","париж","лондон","берлин","рим","токио","пекин","нью-йорк","каир","мехико"));
        WORDS.put("food", Arrays.asList("пицца","суши","бургер","паста","салат","суп","стейк","омлет","торт","мороженое"));
        WORDS.put("professions", Arrays.asList("врач","учитель","инженер","пилот","повар","художник","музыкант","архитектор","юрист","журналист"));
        WORDS.put("general", Arrays.asList("программирование","алгоритм","компьютер","интернет","игра","код","данные","сервер","сайт","приложение"));
    }

    private static final String[] HANGMAN_PICS = {
        "\n      +---+\n          |\n          |\n          |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n          |\n          |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n          |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n      |   |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n     /|   |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n     /|\\  |\n          |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n     /|\\  |\n     /    |\n          |\n    =========\n",
        "\n      +---+\n      |   |\n      O   |\n     /|\\  |\n     / \\  |\n          |\n    =========\n"
    };

    static class Stats {
        int games, wins, losses, streak, bestStreak;
    }

    private int maxErrors;
    private String theme;
    private String word;
    private char[] display;
    private Set<Character> guessed = new HashSet<>();
    private Set<Character> wrong = new HashSet<>();
    private int errors;
    private boolean hintUsed;
    private boolean gameOver;
    private boolean won;
    private Stats stats;
    private String statsFile;
    private Scanner scanner;

    public hangman(int maxErr, String th) {
        maxErrors = maxErr;
        theme = th;
        statsFile = System.getProperty("user.home") + "/.hangman_stats.json";
        loadStats();
        List<String> words = WORDS.getOrDefault(theme, WORDS.get("general"));
        Random rnd = new Random();
        word = words.get(rnd.nextInt(words.size()));
        display = word.toCharArray();
        for (int i = 0; i < display.length; i++) display[i] = '_';
        scanner = new Scanner(System.in);
    }

    private void loadStats() {
        stats = new Stats();
        try {
            String json = new String(Files.readAllBytes(Paths.get(statsFile)));
            stats.games = extractInt(json, "games");
            stats.wins = extractInt(json, "wins");
            stats.losses = extractInt(json, "losses");
            stats.streak = extractInt(json, "streak");
            stats.bestStreak = extractInt(json, "best_streak");
        } catch (Exception e) {
            // file not exists
        }
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0;
        int start = json.indexOf(":", idx) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        try { return Integer.parseInt(json.substring(start, end).trim()); } catch (Exception e) { return 0; }
    }

    private void saveStats() {
        try {
            String json = "{\"games\":" + stats.games + ",\"wins\":" + stats.wins +
                          ",\"losses\":" + stats.losses + ",\"streak\":" + stats.streak +
                          ",\"best_streak\":" + stats.bestStreak + "}";
            Files.write(Paths.get(statsFile), json.getBytes());
        } catch (IOException e) {}
    }

    private void displayState() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println(colorize("🎯 ВИСЕЛИЦА", BOLD));
        System.out.println(colorize("Тема: " + theme, BLUE));
        String col = errors > 0 ? RED : GREEN;
        System.out.println(colorize("Ошибок: " + errors + "/" + maxErrors, col));
        System.out.println(HANGMAN_PICS[errors]);
        System.out.print("Слово: ");
        for (char c : display) System.out.print(c + " ");
        System.out.println();
        if (!guessed.isEmpty()) {
            System.out.print(colorize("Открытые буквы: ", GREEN));
            for (char c : guessed) System.out.print(c + " ");
            System.out.println();
        }
        if (!wrong.isEmpty()) {
            System.out.print(colorize("Ошибочные: ", RED));
            for (char c : wrong) System.out.print(c + " ");
            System.out.println();
        }
    }

    private boolean guess(char letter) {
        letter = Character.toLowerCase(letter);
        if (guessed.contains(letter) || wrong.contains(letter)) {
            System.out.println(colorize("Вы уже называли эту букву.", YELLOW));
            return false;
        }
        if (word.indexOf(letter) != -1) {
            guessed.add(letter);
            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) == letter) display[i] = letter;
            }
            if (new String(display).indexOf('_') == -1) {
                won = true;
                gameOver = true;
            }
            return true;
        } else {
            wrong.add(letter);
            errors++;
            if (errors >= maxErrors) gameOver = true;
            return false;
        }
    }

    private void hint() {
        if (hintUsed) {
            System.out.println(colorize("Подсказка уже использована.", YELLOW));
            return;
        }
        for (int i = 0; i < display.length; i++) {
            if (display[i] == '_') {
                display[i] = word.charAt(i);
                guessed.add(word.charAt(i));
                hintUsed = true;
                System.out.println(colorize("Подсказка: буква '" + word.charAt(i) + "' открыта.", CYAN));
                if (new String(display).indexOf('_') == -1) {
                    won = true;
                    gameOver = true;
                }
                break;
            }
        }
    }

    public void play() {
        System.out.println(colorize("Добро пожаловать в Виселицу!", BOLD));
        System.out.println("Вводите буквы. ? - подсказка. quit - выход.");
        while (!gameOver) {
            displayState();
            System.out.print("> ");
            String cmd = scanner.nextLine().trim();
            if (cmd.equals("quit")) {
                System.out.println("Выход.");
                return;
            }
            if (cmd.equals("?")) {
                hint();
                continue;
            }
            if (cmd.length() != 1 || !Character.isLetter(cmd.charAt(0))) {
                System.out.println(colorize("Введите одну букву.", RED));
                continue;
            }
            guess(cmd.charAt(0));
        }
        displayState();
        if (won) {
            System.out.println(colorize("🎉 Поздравляем! Вы выиграли!", GREEN));
            stats.wins++;
            stats.streak++;
            if (stats.streak > stats.bestStreak) stats.bestStreak = stats.streak;
        } else {
            System.out.println(colorize("💀 Вы проиграли. Загаданное слово: " + word, RED));
            stats.losses++;
            stats.streak = 0;
        }
        stats.games++;
        saveStats();
        System.out.println(colorize("Статистика: игр " + stats.games + ", побед " + stats.wins +
                                    ", поражений " + stats.losses + ", серия " + stats.streak +
                                    ", лучшая " + stats.bestStreak, BLUE));
        scanner.close();
    }

    public static void main(String[] args) {
        int maxErrors = 10;
        String theme = "general";
        boolean showStats = false, resetStats = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("easy")) maxErrors = 10;
            else if (arg.equals("medium")) maxErrors = 7;
            else if (arg.equals("hard")) maxErrors = 5;
            else if (arg.equals("-t") || arg.equals("--theme")) {
                if (i+1 < args.length) {
                    theme = args[++i];
                    if (!WORDS.containsKey(theme)) theme = "general";
                }
            } else if (arg.equals("-s") || arg.equals("--stats")) showStats = true;
            else if (arg.equals("-r") || arg.equals("--reset")) resetStats = true;
            else if (arg.equals("-h") || arg.equals("--help")) {
                System.out.println("Usage: java hangman [easy|medium|hard] [-t theme] [-s] [-r]");
                return;
            }
        }
        if (resetStats) {
            String f = System.getProperty("user.home") + "/.hangman_stats.json";
            try { Files.deleteIfExists(Paths.get(f)); } catch (Exception e) {}
            System.out.println("Статистика сброшена.");
            return;
        }
        if (showStats) {
            String f = System.getProperty("user.home") + "/.hangman_stats.json";
            try {
                String json = new String(Files.readAllBytes(Paths.get(f)));
                int games = extractInt(json, "games");
                int wins = extractInt(json, "wins");
                int losses = extractInt(json, "losses");
                int streak = extractInt(json, "streak");
                int best = extractInt(json, "best_streak");
                System.out.println(colorize("📊 Статистика:", BOLD));
                System.out.println("  Игр: " + games);
                System.out.println("  Побед: " + wins);
                System.out.println("  Поражений: " + losses);
                System.out.println("  Текущая серия: " + streak);
                System.out.println("  Лучшая серия: " + best);
            } catch (Exception e) {
                System.out.println("Статистика пуста.");
            }
            return;
        }
        hangman game = new hangman(maxErrors, theme);
        game.play();
    }

    private static int extractInt(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return 0;
        int start = json.indexOf(":", idx) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        try { return Integer.parseInt(json.substring(start, end).trim()); } catch (Exception e) { return 0; }
    }
}
