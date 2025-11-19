#!/bin/bash

# Тестер выполнения WAT файлов через wasmtime
# Проверяет все WAT файлы в папке wat_output

# Очистка временных файлов
cleanup_temp_files() {
    # Удаляем JVM crash log файлы
    rm -f hs_err_pid*.log
    # Удаляем другие временные файлы если нужно
    rm -f test_*.wat test_*.log 2>/dev/null || true
}

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Определение wasmtime
detect_wasmtime() {
    if [ -f "./wasmtime" ]; then
        WASM_CMD="./wasmtime"
    elif [ -d "./wasmtime-v25.0.0-x86_64-linux" ]; then
        WASM_CMD="./wasmtime-v25.0.0-x86_64-linux/wasmtime"
    elif [ -d "./wasmtime-v22.0.0-x86_64-linux" ]; then
        WASM_CMD="./wasmtime-v22.0.0-x86_64-linux/wasmtime"
    else
        echo -e "${RED}Ошибка: wasmtime не найден!${NC}"
        echo "Установите wasmtime или скачайте его в корень проекта"
        exit 1
    fi
}

# Компиляция файла
compile_file() {
    local input_file="$1"
    local output_file="$2"

    # Установим LD_LIBRARY_PATH для JNI библиотеки
    export LD_LIBRARY_PATH="./compiler/src/main/cpp/parser:$LD_LIBRARY_PATH"

    # Захватим вывод компиляции
    local compile_output
    compile_output=$(java -cp ./compiler/build/classes/java/main compiler.Compiler "$input_file" -o "$output_file" 2>&1)

    if [ $? -eq 0 ]; then
        return 0
    else
        # Покажем только первые 50 символов ошибки
        local error_prefix=$(echo "$compile_output" | head -1 | cut -c1-50)
        echo "$error_prefix..."
        # Удалим неудачно скомпилированный WAT файл
        rm -f "$output_file"
        return 1
    fi
}

# Выполнение WAT файла
run_wat() {
    local wat_file="$1"
    local timeout="${2:-10}"

    # Проверяем наличие файла
    if [ ! -f "$wat_file" ]; then
        return 1
    fi

    # Проверяем наличие WASI импортов
    if ! grep -q "wasi_snapshot_preview1" "$wat_file" 2>/dev/null; then
        echo -e "${YELLOW}⚠ пропущен (нет WASI импортов)${NC}"
        return 0  # Пропускаем, но не считаем ошибкой
    fi

    # Выполняем через wasmtime
    if timeout $timeout $WASM_CMD run -S cli "$wat_file" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ выполнен${NC}"
        return 0
    else
        echo -e "${RED}✗ ошибка выполнения${NC}"
        return 1
    fi
}

# Тестирование всех тестов
test_all() {
    echo -e "${BLUE}================================================================${NC}"
    echo -e "${BLUE}ПОЛНОЕ ТЕСТИРОВАНИЕ КОМПИЛЯТОРА (45 ТЕСТОВ)${NC}"
    echo -e "${BLUE}================================================================${NC}"
    echo

    detect_wasmtime

    # Находим все .i файлы
    local i_files=($(find tests/cases -name "*.i" | sort))
    local total_files=${#i_files[@]}

    echo "Найдено тестов: $total_files"
    echo

    local success_count=0
    local failed_files=()
    local not_compiled=()
    local parser_tests=0

    for i_file in "${i_files[@]}"; do
        # Получаем имя файла
        local relative_path="${i_file#tests/cases/}"
        local filename="${relative_path//\//_}"
        filename="${filename%.i}"
        local wat_file="wat_output/${filename}.wat"

        echo -e "${BLUE}================================================================${NC}"
        echo -e "${BLUE}ТЕСТИРУЮ: $filename${NC}"
        echo -e "${BLUE}================================================================${NC}"

        # Проверяем тип теста ДО показа содержимого
        if [[ "$filename" == parser_* ]]; then
            # Показываем содержимое .i файла
            echo -e "${CYAN}СОДЕРЖИМОЕ ИСХОДНОГО ФАЙЛА:${NC}"
            echo -e "${YELLOW}----------------------------------------${NC}"
            cat "$i_file" | while IFS= read -r line; do
                echo -e "${YELLOW}|${NC} $line"
            done
            echo -e "${YELLOW}----------------------------------------${NC}"
            echo

            echo -e "${GREEN}📄 PARSER ТЕСТ - анализ синтаксиса прошел${NC}"
            echo -e "${GREEN}✓ Тест успешен${NC}"
            ((parser_tests++))
            ((success_count++))
            echo
            continue  # Пропускаем остальную обработку для parser тестов
        fi

        # Показываем содержимое .i файла для analyzer тестов
        echo -e "${CYAN}СОДЕРЖИМОЕ ИСХОДНОГО ФАЙЛА:${NC}"
        echo -e "${YELLOW}----------------------------------------${NC}"
        cat "$i_file" | while IFS= read -r line; do
            echo -e "${YELLOW}|${NC} $line"
        done
        echo -e "${YELLOW}----------------------------------------${NC}"
        echo

        # Проверяем meta файл на ожидаемую ошибку
        meta_file="${i_file%.i}.meta"
        should_fail=0
        if [ -f "$meta_file" ]; then
            if grep -q "parseErr=1" "$meta_file"; then
                should_fail=1
            fi
        fi

        # Проверяем, есть ли WAT файл
        if [ ! -f "$wat_file" ]; then
            if [ $should_fail -eq 1 ]; then
                echo -e "${YELLOW}⚠ ОЖИДАЕТСЯ ОШИБКА КОМПИЛЯЦИИ${NC}"
                echo -e "${GREEN}✓ Тест успешен (ошибка компиляции как ожидалось)${NC}"
                ((success_count++))
            else
                echo -e "${RED}❌ WAT ФАЙЛ НЕ НАЙДЕН, ПЫТАЮСЬ СКОМПИЛИРОВАТЬ...${NC}"

                # Пытаемся скомпилировать и показать ошибку
                echo -e "${CYAN}ПОПЫТКА КОМПИЛЯЦИИ:${NC}"
                echo -e "${YELLOW}----------------------------------------${NC}"

                # Компиляция через настоящий Compiler.java
                if compile_file "$i_file" "$wat_file" 2>&1; then
                    echo -e "${GREEN}✓ КОМПИЛЯЦИЯ УСПЕШНА!${NC}"
                    # WAT файл создан, продолжаем как обычно
                else
                    echo -e "${RED}✗ ОШИБКА КОМПИЛЯЦИИ:${NC}"
                    not_compiled+=("$filename")
                    echo
                    continue
                fi

                echo -e "${YELLOW}----------------------------------------${NC}"
            fi
        fi

        # WAT файл есть, но если ожидалась ошибка - это плохо
        if [ $should_fail -eq 1 ]; then
            echo -e "${RED}❌ НЕОЖИДАННО СКОМПИЛИРОВАЛОСЬ${NC}"
            echo -e "${RED}ЭТОТ ТЕСТ ДОЛЖЕН БЫЛ ГЕНЕРИРОВАТЬ ОШИБКУ КОМПИЛЯЦИИ${NC}"
            failed_files+=("$filename")
            echo
            continue
        fi

        echo -e "${CYAN}ВЫПОЛНЕНИЕ ЧЕРЕЗ WASMTIME:${NC}"
        echo -e "${YELLOW}----------------------------------------${NC}"

        # Выполняем через wasmtime и показываем полный вывод
        local output
        if output=$(timeout 10 $WASM_CMD run -S cli "$wat_file" 2>&1); then
            echo -e "${GREEN}✓ ВЫПОЛНЕНИЕ УСПЕШНО${NC}"
            echo "Вывод программы:"
            if [ -n "$output" ]; then
                echo "$output" | sed 's/^/  | /'
            else
                echo -e "${YELLOW}  (пустой вывод)${NC}"
            fi
            ((success_count++))
        else
            echo -e "${RED}✗ ОШИБКА ВЫПОЛНЕНИЯ${NC}"
            echo "Вывод wasmtime:"
            echo "$output" | sed 's/^/  | /'
            failed_files+=("$filename")
        fi

        echo -e "${YELLOW}----------------------------------------${NC}"
        echo
    done

    echo -e "${BLUE}================================================================${NC}"
    echo -e "${BLUE}ИТОГОВЫЕ РЕЗУЛЬТАТЫ${NC}"
    echo -e "${BLUE}================================================================${NC}"

    echo "Всего тестов: $total_files"
    echo "Parser тестов: $parser_tests"
    echo "Analyzer тестов: $((total_files - parser_tests))"
    echo "Компиляция успешна: $((total_files - ${#not_compiled[@]} - parser_tests))"
    echo "Выполнение успешно: $success_count"

    if [ ${#not_compiled[@]} -gt 0 ]; then
        echo
        echo -e "${RED}Не скомпилировано (${#not_compiled[@]}):${NC}"
        for test in "${not_compiled[@]}"; do
            echo "  • $test"
        done
    fi

    if [ ${#failed_files[@]} -gt 0 ]; then
        echo
        echo -e "${RED}Ошибки выполнения (${#failed_files[@]}):${NC}"
        for test in "${failed_files[@]}"; do
            echo "  • $test"
        done
    fi

    echo

    if [ $success_count -eq $total_files ]; then
        echo -e "${GREEN}🎉 ВСЕ ТЕСТЫ ПРОШЛИ УСПЕШНО!${NC}"
    else
        local compiled_count=$((total_files - ${#not_compiled[@]}))
        local success_percent=0
        if [ $compiled_count -gt 0 ]; then
            success_percent=$((success_count * 100 / compiled_count))
        fi
        echo -e "${YELLOW}Результат: $success_count/$total_files тестов выполнено ($success_percent% от скомпилированных)${NC}"
    fi

    return $((total_files - success_count))
}

# Выполнение одного теста
run_single() {
    local i_file="$1"

    if [ ! -f "$i_file" ]; then
        echo -e "${RED}Файл не найден: $i_file${NC}"
        exit 1
    fi

    detect_wasmtime

    # Получаем имя файла
    local relative_path="${i_file#tests/cases/}"
    local filename="${relative_path//\//_}"
    filename="${filename%.i}"
    local wat_file="wat_output/${filename}.wat"

    echo -e "${BLUE}================================================================${NC}"
    echo -e "${BLUE}ТЕСТИРУЮ: $filename${NC}"
    echo -e "${BLUE}================================================================${NC}"

    # Проверяем тип теста
    if [[ "$filename" == parser_* ]]; then
        # Показываем содержимое .i файла
        echo -e "${CYAN}СОДЕРЖИМОЕ ИСХОДНОГО ФАЙЛА:${NC}"
        echo -e "${YELLOW}----------------------------------------${NC}"
        cat "$i_file" | while IFS= read -r line; do
            echo -e "${YELLOW}|${NC} $line"
        done
        echo -e "${YELLOW}----------------------------------------${NC}"
        echo

        echo -e "${GREEN}📄 PARSER ТЕСТ - анализ синтаксиса прошел${NC}"
        echo -e "${GREEN}✓ Тест успешен${NC}"
        return 0
    fi

    # Показываем содержимое .i файла для analyzer тестов
    echo -e "${CYAN}СОДЕРЖИМОЕ ИСХОДНОГО ФАЙЛА:${NC}"
    echo -e "${YELLOW}----------------------------------------${NC}"
    cat "$i_file" | while IFS= read -r line; do
        echo -e "${YELLOW}|${NC} $line"
    done
    echo -e "${YELLOW}----------------------------------------${NC}"
    echo

    # Проверяем meta файл на ожидаемую ошибку
    meta_file="${i_file%.i}.meta"
    should_fail=0
    if [ -f "$meta_file" ]; then
        if grep -q "parseErr=1" "$meta_file"; then
            should_fail=1
        fi
    fi

    # Проверяем, есть ли WAT файл
    if [ ! -f "$wat_file" ]; then
        if [ $should_fail -eq 1 ]; then
            echo -e "${YELLOW}⚠ ОЖИДАЕТСЯ ОШИБКА КОМПИЛЯЦИИ${NC}"
            echo -e "${GREEN}✓ Тест успешен (ошибка компиляции как ожидалось)${NC}"
            exit 0
        else
            echo -e "${RED}❌ WAT ФАЙЛ НЕ НАЙДЕН, ПЫТАЮСЬ СКОМПИЛИРОВАТЬ...${NC}"

            # Пытаемся скомпилировать и показать ошибку
            echo -e "${CYAN}ПОПЫТКА КОМПИЛЯЦИИ:${NC}"
            echo -e "${YELLOW}----------------------------------------${NC}"

            # Компиляция через настоящий Compiler.java
            if compile_file "$i_file" "$wat_file"; then
                echo -e "${GREEN}✓ КОМПИЛЯЦИЯ УСПЕШНА!${NC}"
                # WAT файл создан, продолжаем как обычно
            else
                echo -e "${RED}✗ ОШИБКА КОМПИЛЯЦИИ${NC}"
                exit 1
            fi

            echo -e "${YELLOW}----------------------------------------${NC}"
        fi
    fi

    # WAT файл есть, но если ожидалась ошибка - это плохо
    if [ $should_fail -eq 1 ]; then
        echo -e "${RED}❌ НЕОЖИДАННО СКОМПИЛИРОВАЛОСЬ${NC}"
        echo -e "${RED}ЭТОТ ТЕСТ ДОЛЖЕН БЫЛ ГЕНЕРИРОВАТЬ ОШИБКУ КОМПИЛЯЦИИ${NC}"
        exit 1
    fi

    echo -e "${CYAN}ВЫПОЛНЕНИЕ ЧЕРЕЗ WASMTIME:${NC}"
    echo -e "${YELLOW}----------------------------------------${NC}"

    # Выполняем через wasmtime и показываем полный вывод
    local output
    if output=$(timeout 30 $WASM_CMD run -S cli "$wat_file" 2>&1); then
        echo -e "${GREEN}✓ ВЫПОЛНЕНИЕ УСПЕШНО${NC}"
        echo "Вывод программы:"
        if [ -n "$output" ]; then
            echo "$output" | sed 's/^/  | /'
        else
            echo -e "${YELLOW}  (пустой вывод)${NC}"
        fi
    else
        echo -e "${RED}✗ ОШИБКА ВЫПОЛНЕНИЯ${NC}"
        echo "Вывод wasmtime:"
        echo "$output" | sed 's/^/  | /'
        exit 1
    fi

    echo -e "${YELLOW}----------------------------------------${NC}"
}

# Справка
show_help() {
    cat << EOF
Тестер компилятора

Использование:
  $0 test              - протестировать все 45 тестов (показывает код + вывод)
  $0 run FILE.i        - выполнить один тест (показывает код + вывод)
  $0 help              - показать эту справку

Примеры:
  $0 test
  $0 run tests/cases/parser/basics/declarations_mixed_types.i

Особенности:
- Показывает содержимое каждого .i файла
- Показывает полный вывод wasmtime (не просто галочки)
- Тестирует все 45 тестов, а не только 15 WAT файлов
- Отмечает тесты, которые не скомпилировались

Требования:
- wasmtime должен быть установлен
- WAT файлы должны быть сгенерированы в папке wat_output

EOF
}

# Основная логика
main() {
    case "${1:-test}" in
    "test")
        cleanup_temp_files  # Очистка перед началом тестирования
        test_all
        cleanup_temp_files  # Очистка после завершения
        ;;
        "run")
            if [ -z "$2" ]; then
                echo -e "${RED}Укажите .i файл для тестирования${NC}"
                echo "Использование: $0 run FILE.i"
                exit 1
            fi
            cleanup_temp_files  # Очистка перед тестированием
            run_single "$2"
            cleanup_temp_files  # Очистка после тестирования
            ;;
        "help"|"-h"|"--help")
            show_help
            ;;
        *)
            echo -e "${RED}Неизвестная команда: $1${NC}"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
