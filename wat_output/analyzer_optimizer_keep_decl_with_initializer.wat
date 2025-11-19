(module
(memory 1)
(export "memory" (memory 0))
(global $heap_ptr (mut i32) (i32.const 0x1000))
  (func $alloc (param $size i32) (result i32)
    (local $ptr i32)
    (local $aligned_ptr i32)
    (global.get $heap_ptr)
    local.set $ptr
    local.get $ptr
    i32.const 3
    i32.add
    i32.const 4
    i32.const -1
    i32.xor
    i32.and
    local.set $aligned_ptr
    local.get $aligned_ptr
    local.get $size
    i32.add
    (global.set $heap_ptr)
    local.get $aligned_ptr
  )
(func $print_int (param $value i32)
  (local $temp i32)
  (local $is_negative i32)
  (local $pos i32)
  local.get $value
  i32.const 0
  i32.lt_s
  local.set $is_negative
  local.get $is_negative
  if
    i32.const 45
    call $print_char
    local.get $value
    i32.const -1
    i32.mul
    local.set $value
  end
  local.get $value
  i32.eqz
  if
    i32.const 48
    call $print_char
    return
  end
  i32.const 0
  local.set $pos
  local.get $value
  local.set $temp
  block $digit_done
  loop $digit_loop
    local.get $temp
    i32.eqz
    br_if $digit_done
    local.get $temp
    i32.const 10
    i32.rem_s
    i32.const 48
    i32.add
    (global.get $print_buffer)
    local.get $pos
    i32.add
    i32.store8
    local.get $pos
    i32.const 1
    i32.add
    local.set $pos
    local.get $temp
    i32.const 10
    i32.div_s
    local.set $temp
    br $digit_loop
  end
  end
  block $print_done
  loop $print_loop
    local.get $pos
    i32.eqz
    br_if $print_done
    local.get $pos
    i32.const 1
    i32.sub
    local.set $pos
    (global.get $print_buffer)
    local.get $pos
    i32.add
    i32.load8_u
    call $print_char
    br $print_loop
  end
  end
  return
)
(func $print_char (param $char i32)
  (global.get $print_buffer)
  local.get $char
  i32.store8
  (global.get $iovec_buffer)
  (global.get $print_buffer)
  i32.store
  (global.get $iovec_buffer)
  i32.const 4
  i32.add
  i32.const 1
  i32.store
  i32.const 1
  (global.get $iovec_buffer)
  i32.const 1
  i32.const 0
  call $fd_write
  drop
)
(global $print_buffer (mut i32) (i32.const 0))
(global $print_buffer_size (mut i32) (i32.const 32))
(global $iovec_buffer (mut i32) (i32.const 0))
(func $init_print_buffer
  (global.get $print_buffer)
  i32.eqz
  if
    (global.get $print_buffer_size)
    call $alloc
    (global.set $print_buffer)
  end
  (global.get $iovec_buffer)
  i32.eqz
  if
    i32.const 8
    call $alloc
    (global.set $iovec_buffer)
  end
)
(func $_start
  call $init_print_buffer
)
(export "_start" (func $_start))
)
