(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory 1)
  (export "memory" (memory 0))
  (global $heap_ptr (mut i32) (i32.const 0x10000))
  (global $print_buffer i32 (i32.const 0x1000))
  (global $iovec_buffer i32 (i32.const 0x1010))
  (global $nwritten i32 (i32.const 0x1020))
  (func $init_print_buffer
  )
  (func $alloc (param $size i32) (result i32)
    global.get $heap_ptr
    global.get $heap_ptr
    local.get $size
    i32.add
    global.set $heap_ptr
  )
  (func $print_int (param $n i32)
    local.get $n
    i32.const 0
    i32.lt_s
    if
      i32.const 45
      call $print_char
      local.get $n
      i32.const -1
      i32.mul
      local.set $n
    end
    local.get $n
    call $print_digits
  )
  (func $print_digits (param $n i32)
    local.get $n
    i32.const 10
    i32.lt_u
    if
      local.get $n
      i32.const 48
      i32.add
      call $print_char
    else
      local.get $n
      i32.const 10
      i32.div_u
      call $print_digits
      local.get $n
      i32.const 10
      i32.rem_u
      i32.const 48
      i32.add
      call $print_char
    end
  )
  (func $print_char (param $char i32)
    global.get $print_buffer
    local.get $char
    i32.store8
    global.get $iovec_buffer
    global.get $print_buffer
    i32.store
    global.get $iovec_buffer
    i32.const 1
    i32.store offset=4
    i32.const 1
    global.get $iovec_buffer
    i32.const 1
    global.get $nwritten
    call $fd_write
    drop
  )
  (func $_start (local $y: integer i32)
    call $init_print_buffer
    i32.const 1
    local.set $y: integer
    i32.const 1
    call $print_int
  )
  (export "_start" (func $_start))
)
