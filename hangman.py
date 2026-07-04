# hangman.py
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
import random
import json
from pathlib import Path

# ANSI-цвета
COLORS = {
    'reset': '\033[0m',
    'green': '\033[92m',
    'red': '\033[91m',
    'yellow': '\033[93m',
    'blue': '\033[94m',
    'bold': '\033[1m'
}

def colorize(text, color):
    return f"{COLORS.get(color, '')}{text}{COLORS['reset']}"

# Словари по темам
WORDS = {
    'animals': ['кот', 'собака', 'тигр', 'слон', 'жираф', 'дельфин', 'медведь', 'волк', 'лиса', 'заяц'],
    'cities': ['москва', 'париж', 'лондон', 'берлин', 'рим', 'токио', 'пекин', 'нью-йорк', 'каир', 'мехико'],
    'food': ['пицца', 'суши', 'бургер', 'паста', 'салат', 'суп', 'стейк', 'омлет', 'торт', 'мороженое'],
    'professions': ['врач', 'учитель', 'инженер', 'пилот', 'повар', 'художник', 'музыкант', 'архитектор', 'юрист', 'журналист'],
    'general': ['программирование', 'алгоритм', 'компьютер', 'интернет', 'игра', 'код', 'данные', 'сервер', 'сайт', 'приложение']
}

# ASCII-арт виселицы (11 стадий: 0 - пусто, 10 - полная)
HANGMAN_PICS = [
    """
      +---+
          |
          |
          |
          |
          |
    =========
    """,
    """
      +---+
      |   |
          |
          |
          |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
          |
          |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
      |   |
          |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
     /|   |
          |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
     /|\\  |
          |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
     /|\\  |
     /    |
          |
    =========
    """,
    """
      +---+
      |   |
      O   |
     /|\\  |
     / \\  |
          |
    =========
    """
]

class Hangman:
    def __init__(self, max_errors=10, theme='general'):
        self.max_errors = max_errors
        self.theme = theme
        self.word = random.choice(WORDS.get(theme, WORDS['general']))
        self.display = ['_'] * len(self.word)
        self.guessed = set()
        self.wrong = set()
        self.errors = 0
        self.hint_used = False
        self.game_over = False
        self.won = False
        self.stats_file = Path.home() / '.hangman_stats.json'
        self.load_stats()

    def load_stats(self):
        if self.stats_file.exists():
            with open(self.stats_file, 'r') as f:
                self.stats = json.load(f)
        else:
            self.stats = {'games': 0, 'wins': 0, 'losses': 0, 'streak': 0, 'best_streak': 0}

    def save_stats(self):
        with open(self.stats_file, 'w') as f:
            json.dump(self.stats, f, indent=2)

    def display(self):
        os.system('clear' if os.name == 'posix' else 'cls')
        print(colorize("🎯 ВИСЕЛИЦА", 'bold'))
        print(colorize(f"Тема: {self.theme}", 'blue'))
        print(colorize(f"Ошибок: {self.errors}/{self.max_errors}", 'red' if self.errors > 0 else 'green'))
        print(HANGMAN_PICS[self.errors])
        print("Слово: " + ' '.join(self.display))
        if self.guessed:
            print(colorize(f"Открытые буквы: {', '.join(sorted(self.guessed))}", 'green'))
        if self.wrong:
            print(colorize(f"Ошибочные: {', '.join(sorted(self.wrong))}", 'red'))

    def guess(self, letter):
        if letter in self.guessed or letter in self.wrong:
            print(colorize("Вы уже называли эту букву.", 'yellow'))
            return False
        if letter in self.word:
            self.guessed.add(letter)
            for i, ch in enumerate(self.word):
                if ch == letter:
                    self.display[i] = ch
            if '_' not in self.display:
                self.won = True
                self.game_over = True
            return True
        else:
            self.wrong.add(letter)
            self.errors += 1
            if self.errors >= self.max_errors:
                self.game_over = True
            return False

    def hint(self):
        if self.hint_used:
            print(colorize("Подсказка уже использована.", 'yellow'))
            return
        # Находим первую неоткрытую букву
        for i, ch in enumerate(self.word):
            if self.display[i] == '_':
                self.display[i] = ch
                self.guessed.add(ch)
                self.hint_used = True
                print(colorize(f"Подсказка: буква '{ch}' открыта.", 'cyan'))
                if '_' not in self.display:
                    self.won = True
                    self.game_over = True
                break

    def play(self):
        print(colorize("Добро пожаловать в Виселицу!", 'bold'))
        print("Вводите буквы. ? - подсказка. quit - выход.")
        while not self.game_over:
            self.display()
            cmd = input("> ").strip().lower()
            if cmd == 'quit':
                print("Выход.")
                return
            if cmd == '?':
                self.hint()
                continue
            if len(cmd) != 1 or not cmd.isalpha():
                print(colorize("Введите одну букву.", 'red'))
                continue
            self.guess(cmd)
        # Конец игры
        self.display()
        if self.won:
            print(colorize("🎉 Поздравляем! Вы выиграли!", 'green'))
            self.stats['wins'] += 1
            self.stats['streak'] += 1
            if self.stats['streak'] > self.stats['best_streak']:
                self.stats['best_streak'] = self.stats['streak']
        else:
            print(colorize(f"💀 Вы проиграли. Загаданное слово: {self.word}", 'red'))
            self.stats['losses'] += 1
            self.stats['streak'] = 0
        self.stats['games'] += 1
        self.save_stats()
        print(colorize(f"Статистика: игр {self.stats['games']}, побед {self.stats['wins']}, поражений {self.stats['losses']}, серия {self.stats['streak']}, лучшая {self.stats['best_streak']}", 'blue'))

def main():
    max_errors = 10
    theme = 'general'
    show_stats = False
    reset_stats = False
    args = sys.argv[1:]
    for i, arg in enumerate(args):
        if arg in ['easy', 'medium', 'hard']:
            if arg == 'easy': max_errors = 10
            elif arg == 'medium': max_errors = 7
            elif arg == 'hard': max_errors = 5
        elif arg == '-t' or arg == '--theme':
            if i+1 < len(args):
                theme = args[i+1]
                if theme not in WORDS:
                    theme = 'general'
        elif arg == '-s' or arg == '--stats':
            show_stats = True
        elif arg == '-r' or arg == '--reset':
            reset_stats = True
        elif arg == '-h' or arg == '--help':
            print("Usage: hangman.py [easy|medium|hard] [-t theme] [-s] [-r]")
            return
    if reset_stats:
        stats_file = Path.home() / '.hangman_stats.json'
        if stats_file.exists():
            stats_file.unlink()
        print("Статистика сброшена.")
        return
    if show_stats:
        stats_file = Path.home() / '.hangman_stats.json'
        if stats_file.exists():
            with open(stats_file, 'r') as f:
                stats = json.load(f)
                print(colorize("📊 Статистика:", 'bold'))
                print(f"  Игр: {stats['games']}")
                print(f"  Побед: {stats['wins']}")
                print(f"  Поражений: {stats['losses']}")
                print(f"  Текущая серия: {stats['streak']}")
                print(f"  Лучшая серия: {stats['best_streak']}")
        else:
            print("Статистика пуста.")
        return
    game = Hangman(max_errors, theme)
    game.play()

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print(colorize("\nИгра прервана.", 'yellow'))
        sys.exit(0)
