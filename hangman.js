// hangman.js
#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const readline = require('readline');

const COLORS = {
    reset: '\x1b[0m',
    green: '\x1b[92m',
    red: '\x1b[91m',
    yellow: '\x1b[93m',
    blue: '\x1b[94m',
    cyan: '\x1b[96m',
    bold: '\x1b[1m'
};

function colorize(text, color) {
    return COLORS[color] + text + COLORS.reset;
}

const WORDS = {
    animals: ['кот', 'собака', 'тигр', 'слон', 'жираф', 'дельфин', 'медведь', 'волк', 'лиса', 'заяц'],
    cities: ['москва', 'париж', 'лондон', 'берлин', 'рим', 'токио', 'пекин', 'нью-йорк', 'каир', 'мехико'],
    food: ['пицца', 'суши', 'бургер', 'паста', 'салат', 'суп', 'стейк', 'омлет', 'торт', 'мороженое'],
    professions: ['врач', 'учитель', 'инженер', 'пилот', 'повар', 'художник', 'музыкант', 'архитектор', 'юрист', 'журналист'],
    general: ['программирование', 'алгоритм', 'компьютер', 'интернет', 'игра', 'код', 'данные', 'сервер', 'сайт', 'приложение']
};

const HANGMAN_PICS = [
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
     /|\\  |
          |
          |
    =========
    `,
    `
      +---+
      |   |
      O   |
     /|\\  |
     /    |
          |
    =========
    `,
    `
      +---+
      |   |
      O   |
     /|\\  |
     / \\  |
          |
    =========
    `
];

class Hangman {
    constructor(maxErrors = 10, theme = 'general') {
        this.maxErrors = maxErrors;
        this.theme = theme;
        const words = WORDS[theme] || WORDS.general;
        this.word = words[Math.floor(Math.random() * words.length)];
        this.display = Array(this.word.length).fill('_');
        this.guessed = new Set();
        this.wrong = new Set();
        this.errors = 0;
        this.hintUsed = false;
        this.gameOver = false;
        this.won = false;
        this.statsFile = path.join(os.homedir(), '.hangman_stats.json');
        this.loadStats();
    }

    loadStats() {
        try {
            this.stats = JSON.parse(fs.readFileSync(this.statsFile, 'utf8'));
        } catch {
            this.stats = { games: 0, wins: 0, losses: 0, streak: 0, bestStreak: 0 };
        }
    }

    saveStats() {
        fs.writeFileSync(this.statsFile, JSON.stringify(this.stats, null, 2));
    }

    displayState() {
        console.clear();
        console.log(colorize('🎯 ВИСЕЛИЦА', 'bold'));
        console.log(colorize(`Тема: ${this.theme}`, 'blue'));
        const col = this.errors > 0 ? 'red' : 'green';
        console.log(colorize(`Ошибок: ${this.errors}/${this.maxErrors}`, col));
        console.log(HANGMAN_PICS[this.errors]);
        console.log('Слово: ' + this.display.join(' '));
        if (this.guessed.size) {
            console.log(colorize(`Открытые буквы: ${[...this.guessed].join(', ')}`, 'green'));
        }
        if (this.wrong.size) {
            console.log(colorize(`Ошибочные: ${[...this.wrong].join(', ')}`, 'red'));
        }
    }

    guess(letter) {
        letter = letter.toLowerCase();
        if (this.guessed.has(letter) || this.wrong.has(letter)) {
            console.log(colorize('Вы уже называли эту букву.', 'yellow'));
            return false;
        }
        if (this.word.includes(letter)) {
            this.guessed.add(letter);
            for (let i = 0; i < this.word.length; i++) {
                if (this.word[i] === letter) this.display[i] = letter;
            }
            if (!this.display.includes('_')) {
                this.won = true;
                this.gameOver = true;
            }
            return true;
        } else {
            this.wrong.add(letter);
            this.errors++;
            if (this.errors >= this.maxErrors) this.gameOver = true;
            return false;
        }
    }

    hint() {
        if (this.hintUsed) {
            console.log(colorize('Подсказка уже использована.', 'yellow'));
            return;
        }
        for (let i = 0; i < this.display.length; i++) {
            if (this.display[i] === '_') {
                this.display[i] = this.word[i];
                this.guessed.add(this.word[i]);
                this.hintUsed = true;
                console.log(colorize(`Подсказка: буква '${this.word[i]}' открыта.`, 'cyan'));
                if (!this.display.includes('_')) {
                    this.won = true;
                    this.gameOver = true;
                }
                break;
            }
        }
    }

    async play() {
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        const question = (q) => new Promise(resolve => rl.question(q, resolve));

        console.log(colorize('Добро пожаловать в Виселицу!', 'bold'));
        console.log('Вводите буквы. ? - подсказка. quit - выход.');
        while (!this.gameOver) {
            this.displayState();
            const cmd = (await question('> ')).trim().toLowerCase();
            if (cmd === 'quit') {
                console.log('Выход.');
                rl.close();
                return;
            }
            if (cmd === '?') {
                this.hint();
                continue;
            }
            if (cmd.length !== 1 || !/[a-zа-я]/.test(cmd)) {
                console.log(colorize('Введите одну букву.', 'red'));
                continue;
            }
            this.guess(cmd);
        }
        this.displayState();
        if (this.won) {
            console.log(colorize('🎉 Поздравляем! Вы выиграли!', 'green'));
            this.stats.wins++;
            this.stats.streak++;
            if (this.stats.streak > this.stats.bestStreak) this.stats.bestStreak = this.stats.streak;
        } else {
            console.log(colorize(`💀 Вы проиграли. Загаданное слово: ${this.word}`, 'red'));
            this.stats.losses++;
            this.stats.streak = 0;
        }
        this.stats.games++;
        this.saveStats();
        console.log(colorize(`Статистика: игр ${this.stats.games}, побед ${this.stats.wins}, поражений ${this.stats.losses}, серия ${this.stats.streak}, лучшая ${this.stats.bestStreak}`, 'blue'));
        rl.close();
    }
}

async function main() {
    let maxErrors = 10;
    let theme = 'general';
    let showStats = false;
    let resetStats = false;
    const args = process.argv.slice(2);
    for (let i = 0; i < args.length; i++) {
        const arg = args[i];
        if (arg === 'easy') maxErrors = 10;
        else if (arg === 'medium') maxErrors = 7;
        else if (arg === 'hard') maxErrors = 5;
        else if (arg === '-t' || arg === '--theme') {
            if (i + 1 < args.length) {
                theme = args[++i];
                if (!WORDS[theme]) theme = 'general';
            }
        } else if (arg === '-s' || arg === '--stats') showStats = true;
        else if (arg === '-r' || arg === '--reset') resetStats = true;
        else if (arg === '-h' || arg === '--help') {
            console.log('Usage: node hangman.js [easy|medium|hard] [-t theme] [-s] [-r]');
            process.exit(0);
        }
    }
    if (resetStats) {
        const f = path.join(os.homedir(), '.hangman_stats.json');
        if (fs.existsSync(f)) fs.unlinkSync(f);
        console.log('Статистика сброшена.');
        return;
    }
    if (showStats) {
        const f = path.join(os.homedir(), '.hangman_stats.json');
        try {
            const stats = JSON.parse(fs.readFileSync(f, 'utf8'));
            console.log(colorize('📊 Статистика:', 'bold'));
            console.log(`  Игр: ${stats.games}`);
            console.log(`  Побед: ${stats.wins}`);
            console.log(`  Поражений: ${stats.losses}`);
            console.log(`  Текущая серия: ${stats.streak}`);
            console.log(`  Лучшая серия: ${stats.bestStreak}`);
        } catch {
            console.log('Статистика пуста.');
        }
        return;
    }
    const game = new Hangman(maxErrors, theme);
    await game.play();
}

main().catch(console.error);
