# Nitro notes

Nitro is Bitwig's DSP language for Grid modules; the compiler front end is in `com.bitwig.nitro` (an ANTLR grammar, an AST, LLVM-based code generation). The tools here only parse; **type checking and code generation happen when Bitwig loads the device**, so a clean `nitcheck` does not guarantee a clean load.

## Storage

A Nitro module (class 1721) stores its code in property 10421 as `0x00` followed by a raw-deflate stream of the (tokenised) source. `DocKit.compileNitro` produces it with `NitroFile`; `NitCompile` and `Dump` decode it. The decoded printer omits **event-handler bodies** and port payload types, so decoded sources of other devices are incomplete.

## Syntax confirmed with the real parser

```nitro
component name
{
    audio_inport in;                       // audio ports: no type needed
    audio_inport f32 rate = 0.5;           // or typed with a default
    value_inport i32 shape;                // value ports need a type
    value_inport bool retrigger;
    event_inport NoteEvent note_in { ... } // event ports need a type and a body
    audio_outport out;
    f32 phase = 0.0;                       // state
    process { ... }                        // runs once per sample
}
```

- Ports are declared without semicolon in decoded output but **need `;`** in source (except event ports with a body).
- Casts: `(x).f32`, `(x).i32`, `(x).i64`. Sample rate: `(EC.samplerate).f32`. Math: `sin`, `pow`, `floor`, `fabs`, `fmin`, `fmax`.
- Functions: `f32 name(i32 i) { ... return v; }`.
- Note events: `note_in.type == note_in.NOTE_ON`, `note_in.key`.

## Port types in the document

- **Enum parameters arrive as integers** (`value_inport i32`). Declaring them `f32` makes Bitwig's integer 1, 2, 3… read as denormal floats near zero, so every enum looks like 0.
- **Boolean parameters** use `value_inport bool`.
- Decimal parameters wired to a Nitro input are audio-rate (`audio_inport`).
- Tempo: wire the transport atom's `beats_per_frame` into an audio input.
- Port record fields are listed in [FILE-FORMAT.md](FILE-FORMAT.md). A `NoteEvent` input uses width 0, event kind 44 and the type string `{i32, i32, i32, float, i32, i32, i32, [ 16 x i8 ]}`.

## Known traps (from the blueprint docs and these builds)

- Read a port once per sample into a local; repeated reads of the same port can recurse in the semantic pass.
- Prefer explicit casts.
- `mix()` and `clamp()` are reported traps on this target; use `fmin`/`fmax`.
