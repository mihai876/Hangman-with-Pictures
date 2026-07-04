#!/usr/bin/env ruby
# hangman.rb
# encoding: UTF-8

require 'json'
require 'fileutils'
require 'set'

COLORS = {
  reset: "\e[0m",
  green: "\e[92m",
  red: "\e[91m",
  yellow: "\e[93m",
  blue: "\e[94m",
  cyan: "\e[96m",
  bold: "\e[1m"
}

def colorize(text, color)
  "#{COLORS[color]}#{text}#{COLORS[:reset]}"
end

WORDS = {
  'animals' => %w[кот собака тигр слон жираф дельфин медведь волк лиса заяц],
  'cities' => %w[москва париж лондон берлин рим токио пекин нью-йорк каир мехико],
  'food' => %w[пицца суши бургер паста салат суп стейк омлет торт мороженое],
  'professions' => %w[врач учитель инженер пилот повар художник музыкант архитектор юрист журналист],
  'general' => %w[программирование алгоритм компьютер интернет игра код данные сервер сайт приложение]
}

HANGMAN_PICS = [
  <<~PIC,
      +---+
          |
          |
          |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
          |
          |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
          |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
      |   |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
     /|   |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
     /|\\  |
          |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
     /|\\  |
     /    |
          |
    =========
  PIC
  <<~PIC,
      +---+
      |   |
      O   |
     /|\\  |
     / \\  |
          |
    =========
  PIC
]

class Hangman
  attr_reader :max_errors, :theme, :word, :display, :guessed, :wrong,
              :errors, :hint_used, :game_over, :won, :stats, :stats_file

  def initialize(max_errors = 10, theme = 'general')
    @max_errors = max_errors
    @theme = theme
    @stats_file = File.join(Dir.home, '.hangman_stats.json')
    load_stats
    words = WORDS[theme] || WORDS['general']
    @word = words.sample
    @display = Array.new(@word.length, '_')
    @guessed = Set.new
    @wrong = Set.new
    @errors = 0
    @hint_used = false
    @game_over = false
    @won = false
  end

  def load_stats
    if File.exist?(@stats_file)
      @stats = JSON.parse(File.read(@stats_file))
    else
      @stats = { 'games' => 0, 'wins' => 0, 'losses' => 0, 'streak' => 0, 'best_streak' => 0 }
    end
  end

  def save_stats
    File.write(@stats_file, JSON.pretty_generate(@stats))
  end

  def display_state
    system('clear') || system('cls')
    puts colorize('🎯 ВИСЕЛИЦА', :bold)
    puts colorize("Тема: #{@theme}", :blue)
    col = @errors > 0 ? :red : :green
    puts colorize("Ошибок: #{@errors}/#{@max_errors}", col)
    puts HANGMAN_PICS[@errors]
    puts "Слово: #{@display.join(' ')}"
    unless @guessed.empty?
      puts colorize("Открытые буквы: #{@guessed.to_a.join(', ')}", :green)
    end
    unless @wrong.empty?
      puts colorize("Ошибочные: #{@wrong.to_a.join(', ')}", :red)
    end
  end

  def guess(letter)
    letter = letter.downcase
    if @guessed.include?(letter) || @wrong.include?(letter)
      puts colorize('Вы уже называли эту букву.', :yellow)
      return false
    end
    if @word.include?(letter)
      @guessed.add(letter)
      @word.chars.each_with_index do |ch, i|
        @display[i] = ch if ch == letter
      end
      if !@display.include?('_')
        @won = true
        @game_over = true
      end
      true
    else
      @wrong.add(letter)
      @errors += 1
      @game_over = true if @errors >= @max_errors
      false
    end
  end

  def hint
    if @hint_used
      puts colorize('Подсказка уже использована.', :yellow)
      return
    end
    @display.each_with_index do |ch, i|
      if ch == '_'
        @display[i] = @word[i]
        @guessed.add(@word[i])
        @hint_used = true
        puts colorize("Подсказка: буква '#{@word[i]}' открыта.", :cyan)
        if !@display.include?('_')
          @won = true
          @game_over = true
        end
        break
      end
    end
  end

  def play
    puts colorize('Добро пожаловать в Виселицу!', :bold)
    puts 'Вводите буквы. ? - подсказка. quit - выход.'
    until @game_over
      display_state
      print '> '
      cmd = gets.chomp.strip
      case cmd
      when 'quit'
        puts 'Выход.'
        return
      when '?'
        hint
        next
      else
        if cmd.length != 1 || !cmd.match?(/[[:alpha:]]/)
          puts colorize('Введите одну букву.', :red)
          next
        end
        guess(cmd)
      end
    end
    display_state
    if @won
      puts colorize('🎉 Поздравляем! Вы выиграли!', :green)
      @stats['wins'] += 1
      @stats['streak'] += 1
      if @stats['streak'] > @stats['best_streak']
        @stats['best_streak'] = @stats['streak']
      end
    else
      puts colorize("💀 Вы проиграли. Загаданное слово: #{@word}", :red)
      @stats['losses'] += 1
      @stats['streak'] = 0
    end
    @stats['games'] += 1
    save_stats
    puts colorize("Статистика: игр #{@stats['games']}, побед #{@stats['wins']}, поражений #{@stats['losses']}, серия #{@stats['streak']}, лучшая #{@stats['best_streak']}", :blue)
  end
end

def main
  max_errors = 10
  theme = 'general'
  show_stats = false
  reset_stats = false
  i = 0
  while i < ARGV.size
    arg = ARGV[i]
    case arg
    when 'easy' then max_errors = 10
    when 'medium' then max_errors = 7
    when 'hard' then max_errors = 5
    when '-t', '--theme'
      theme = ARGV[i+1] if i+1 < ARGV.size
      theme = 'general' unless WORDS.key?(theme)
      i += 1
    when '-s', '--stats' then show_stats = true
    when '-r', '--reset' then reset_stats = true
    when '-h', '--help'
      puts 'Usage: ruby hangman.rb [easy|medium|hard] [-t theme] [-s] [-r]'
      return
    end
    i += 1
  end
  if reset_stats
    f = File.join(Dir.home, '.hangman_stats.json')
    File.delete(f) if File.exist?(f)
    puts 'Статистика сброшена.'
    return
  end
  if show_stats
    f = File.join(Dir.home, '.hangman_stats.json')
    if File.exist?(f)
      stats = JSON.parse(File.read(f))
      puts colorize('📊 Статистика:', :bold)
      puts "  Игр: #{stats['games']}"
      puts "  Побед: #{stats['wins']}"
      puts "  Поражений: #{stats['losses']}"
      puts "  Текущая серия: #{stats['streak']}"
      puts "  Лучшая серия: #{stats['best_streak']}"
    else
      puts 'Статистика пуста.'
    end
    return
  end
  game = Hangman.new(max_errors, theme)
  game.play
end

main if __FILE__ == $0
