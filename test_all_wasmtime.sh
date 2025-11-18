#!/bin/bash

echo "=== ПОЛНАЯ СВОДКА ВЫПОЛНЕНИЯ WASMTIME ===" > wasmtime_full_report.txt
echo "Дата и время: $(date)" >> wasmtime_full_report.txt
echo "" >> wasmtime_full_report.txt

echo "=== СТАТИСТИКА ПО КАТЕГОРИЯМ ===" >> wasmtime_full_report.txt
echo "" >> wasmtime_full_report.txt

echo "PARSER ТЕСТЫ:" >> wasmtime_full_report.txt
parser_success=0
parser_total=0

for file in wat_output/parser_*.wat; do
    parser_total=$((parser_total + 1))
    filename=$(basename "$file" .wat)
    output=$(timeout 5 ./wasmtime "$file" 2>&1)
    exit_code=$?
    if [ $exit_code -eq 0 ]; then
        parser_success=$((parser_success + 1))
        status="✅ SUCCESS"
    else
        status="❌ FAILED"
    fi
    echo "$filename: $status | Вывод: '${output:-<нет вывода>}'" >> wasmtime_full_report.txt
done

echo "" >> wasmtime_full_report.txt
echo "ANALYZER ТЕСТЫ:" >> wasmtime_full_report.txt
analyzer_success=0
analyzer_total=0

for file in wat_output/analyzer_*.wat; do
    analyzer_total=$((analyzer_total + 1))
    filename=$(basename "$file" .wat)
    output=$(timeout 5 ./wasmtime "$file" 2>&1)
    exit_code=$?
    if [ $exit_code -eq 0 ]; then
        analyzer_success=$((analyzer_success + 1))
        status="✅ SUCCESS"
    else
        status="❌ FAILED"
    fi
    echo "$filename: $status | Вывод: '${output:-<нет вывода>}'" >> wasmtime_full_report.txt
done

echo "" >> wasmtime_full_report.txt
echo "=== ИТОГОВАЯ СТАТИСТИКА ===" >> wasmtime_full_report.txt
echo "Parser тесты: $parser_success/$parser_total ($(($parser_success * 100 / $parser_total))%)" >> wasmtime_full_report.txt
echo "Analyzer тесты: $analyzer_success/$analyzer_total ($(($analyzer_success * 100 / $analyzer_total))%)" >> wasmtime_full_report.txt
echo "ОБЩИЙ РЕЗУЛЬТАТ: $(($parser_success + $analyzer_success))/$(($parser_total + $analyzer_total)) ($(($(($parser_success + $analyzer_success)) * 100 / $(($parser_total + $analyzer_total))))%)" >> wasmtime_full_report.txt

if [ $(($parser_success + $analyzer_success)) -eq $(($parser_total + $analyzer_total)) ]; then
    echo "" >> wasmtime_full_report.txt
    echo "🎉 ВСЕ ТЕСТЫ ПРОШЛИ УСПЕШНО! КОМПИЛЯТОР РАБОТАЕТ НА 100%!" >> wasmtime_full_report.txt
fi

echo "Отчет сохранен в wasmtime_full_report.txt"
