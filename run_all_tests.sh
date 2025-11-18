#!/bin/bash

# Скрипт для генерации WAT файлов из всех тестов и запуска через wasmtime
# Автор: AI Assistant

echo "=== КОМПИЛЯЦИЯ ВСЕХ ТЕСТОВ В WAT ФАЙЛЫ ==="
echo ""

WASM_CMD="./wasmtime-v22.0.0-x86_64-linux/wasmtime"

TOTAL_TESTS=0
SUCCESS_COMPILE=0
SUCCESS_RUNTIME=0
FAILED_COMPILE=0
FAILED_RUNTIME=0

# Создаем папку для WAT файлов если её нет
mkdir -p wat_output

echo "Компилируем все тесты..."

# Проходим по всем .i файлам
while IFS= read -r test_file; do
    # Получаем имя файла без пути и расширения
    filename=$(basename "$test_file" .i)

    echo -n "Компилируем $filename... "

    # Компилируем в WAT
    if java -cp compiler/build/libs/* compiler.Compiler "$test_file" -o "wat_output/$filename.wat" 2>/dev/null; then
        echo -e "\033[32m✓\033[0m"
        ((SUCCESS_COMPILE++))
        ((TOTAL_TESTS++))

        # Запускаем через wasmtime
        echo -n "  Запускаем $filename через wasmtime... "
        if $WASM_CMD run -S cli "wat_output/$filename.wat" >/dev/null 2>&1; then
            echo -e "\033[32m✓\033[0m"
            ((SUCCESS_RUNTIME++))
        else
            echo -e "\033[31m✗\033[0m"
            ((FAILED_RUNTIME++))
        fi
    else
        echo -e "\033[31m✗\033[0m"
        ((FAILED_COMPILE++))
        ((TOTAL_TESTS++))
    fi
done < <(find tests -name "*.i" | sort)

echo ""
echo "=== СВОДКА РЕЗУЛЬТАТОВ ==="
echo "Всего тестов: $TOTAL_TESTS"
echo "Компилируется успешно: $SUCCESS_COMPILE"
echo "Компилируется с ошибкой: $FAILED_COMPILE"
echo "Выполняется успешно: $SUCCESS_RUNTIME"
echo "Выполняется с ошибкой: $FAILED_RUNTIME"
echo ""
echo "Процент успешной компиляции: $((SUCCESS_COMPILE * 100 / TOTAL_TESTS))%"
echo "Процент успешного выполнения: $((SUCCESS_RUNTIME * 100 / TOTAL_TESTS))%"

if [ "$FAILED_COMPILE" -gt 0 ]; then
    echo ""
    echo "=== ТЕСТЫ С ОШИБКАМИ КОМПИЛЯЦИИ ==="
    while IFS= read -r test_file; do
        filename=$(basename "$test_file" .i)
        if ! java -cp compiler/build/libs/* compiler.Compiler "$test_file" -o "wat_output/$filename.wat" 2>/dev/null; then
            echo "• $filename ($test_file)"
        fi
    done < <(find tests -name "*.i" | sort)
fi

if [ "$FAILED_RUNTIME" -gt 0 ]; then
    echo ""
    echo "=== ТЕСТЫ С ОШИБКАМИ ВЫПОЛНЕНИЯ ==="
    while IFS= read -r test_file; do
        filename=$(basename "$test_file" .i)
        if java -cp compiler/build/libs/* compiler.Compiler "$test_file" -o "wat_output/$filename.wat" 2>/dev/null; then
            if ! $WASM_CMD run -S cli "wat_output/$filename.wat" >/dev/null 2>&1; then
                echo "• $filename ($test_file)"
                # Показываем детали ошибки
                echo "  Ошибка:"
                $WASM_CMD run -S cli "wat_output/$filename.wat" 2>&1 | head -3
                echo ""
            fi
        fi
    done < <(find tests -name "*.i" | sort)
fi
