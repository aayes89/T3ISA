# T3ISA

Máquina virtual ternaria (balanced ternary) inspirada en la computadora soviética Setun (1958-1965).

Implementa una arquitectura completa de 27 trits con CPU, ALU, memoria, kernel, scheduler, assembler y shell.

## Características principales

- Ternario balanceado (`-1`, `0`, `+1`)
- Palabra de 27 trits
- 27 registros de propósito general
- Memoria de 19683 palabras (3⁹)
- Modos kernel y usuario con protección de memoria
- Stack, llamadas a subrutinas, traps e interrupciones
- ALU completa: suma, resta, multiplicación, división, módulo, desplazamientos y operaciones lógicas ternarias
- Sistema operativo mínimo (T3OS): kernel, scheduler, memory manager, syscalls y shell
- Assembler incluido
- Bus de dispositivos y consola

## Estructura del proyecto

src/t3isa/
├── Trit.java              # Trit (-1, 0, +1)
├── TWord.java             # Palabra de 27 trits
├── TALU.java              # Unidad aritmético-lógica
├── TCPU.java              # Procesador + memoria
├── TInstruction.java      # Decodificación de instrucciones
├── TOpcode.java           # Conjunto de instrucciones
├── TAssembler.java        # Ensamblador
├── TKernel.java           # Kernel
├── TScheduler.java        # Planificador
├── TMemoryManager.java    # Gestión de memoria
├── TShell.java            # Shell interactivo
├── T3OS.java              # Sistema operativo
└── T3ISA.java             # Punto de entrada

## Mapa de memoria

  
    *
    * 0      - Boot sector
    * 1..6   - Trap vectors
    * 7..26  - Reserved
    * 27..   - T3OS / programs
    * 7..9   - interrupt vectors
    * 10..26 - reservado
    * 27...  - T3OS
    * 100    - DIVIDE_BY_ZERO
    * 110    - INVALID_MEMORY
    * 120    - INVALID_INSTRUCTION
    * 130    - INVALID_SYSCALL
    * 140    - DEVICE_ERROR
    * 150    - STACK_ERROR
    * 160    - timer interrupt
    * 170    - device interrupt
    * 180    - keyboard interrupt
    *
    * 0..999       KERNEL
    * 1000..15999  USER
    * 16000..19682 STACK
    *
    * La pila crece hacia abajo.


## Conjunto de instrucciones (resumen)

| Código | Instrucción | Descripción                  |
|--------|-------------|------------------------------|
| NOP    | NOP         | No operación                 |
| HALT   | HALT        | Detiene la máquina           |
| MOV    | MOV         | Copia registro               |
| MOVI   | MOVI        | Carga inmediato              |
| ADD    | ADD         | Suma                         |
| SUB    | SUB         | Resta                        |
| NEG    | NEG         | Negación                     |
| MUL    | MUL         | Multiplicación               |
| DIV    | DIV         | División                     |
| MOD    | MOD         | Módulo                       |
| SHL    | SHL         | Desplazamiento izquierda     |
| SHR    | SHR         | Desplazamiento derecha       |
| CMP    | CMP         | Comparación                  |
| TAND   | TAND        | AND ternario                 |
| TOR    | TOR         | OR ternario                  |
| TXOR   | TXOR        | XOR ternario                 |
| TNOT   | TNOT        | NOT ternario                 |
| JMP    | JMP         | Salto incondicional          |
| JNEG   | JNEG        | Salto si negativo            |
| JZERO  | JZERO       | Salto si cero                |
| JPOS   | JPOS        | Salto si positivo            |
| LOAD   | LOAD        | Carga de memoria             |
| STORE  | STORE       | Almacena en memoria          |
| PUSH   | PUSH        | Apila                        |
| POP    | POP         | Desapila                     |
| CALL   | CALL        | Llamada a subrutina          |
| RET    | RET         | Retorno                      |
| IRET   | IRET        | Retorno de interrupción      |
| SYS    | SYS         | Llamada al sistema           |


## Compilación y ejecución

Proyecto NetBeans / Ant.

```bash
# Compilar
ant jar

# Ejecutar
java -jar dist/T3ISA.jar

O desde el IDE NetBeans: Run Project.
```

# Licencia

MIT License
Copyright (c) 2025 Allan (Slam)