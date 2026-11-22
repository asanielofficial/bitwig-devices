# Building

The scripts extract what they need from **your** Bitwig Studio install into `.work/` (git-ignored). Nothing from Bitwig is stored in this repository.

## Prerequisites

- **JDK 21 or newer** (needs `javac` and `jar`; Bitwig's bundled `jre` is not enough). A JDK older than the one Bitwig ships with is fine, because `scripts/setup` lowers the class-file version of the extracted classes.
- A Bitwig Studio install containing `bin\bitwig.jar`, `bin\libs.jar` and `resources\icons`. The default path on Windows is `C:\Program Files\Bitwig Studio`; set `BITWIG_DIR` if yours differs. `BITWIG_JAR`, `LIBS_JAR` and `BITWIG_ICONS` override individual locations (for example a debug build of `bitwig.jar`).
- The template devices listed in [`reference/README.md`](../reference/README.md), placed in `reference/`.

## Windows (PowerShell)

```powershell
cd path\to\bitwig-devices
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build.ps1 tremolo-plus
```

`-NoProfile` skips your PowerShell profile, which avoids unrelated profile output. To use a different Bitwig folder: `$env:BITWIG_DIR = "D:\Apps\Bitwig Studio"` before running.

## bash (macOS, Linux, Git Bash)

```bash
export BITWIG_DIR="/path/to/Bitwig Studio"
scripts/setup.sh
scripts/build.sh tremolo-plus
```

## What the scripts do

| Script | Purpose |
| --- | --- |
| `setup` | Extract `bitwig.jar` and the ANTLR v4 runtime from `libs.jar` into `.work/classes`, remove the root class named `com` (it shadows the `com.` package), lower class-file versions to your JDK, compile `tools/src` and `src/*`. Run again after a Bitwig update. |
| `build <device>` | Read `src/<device>/device.properties`, compile the `.nit` source with Bitwig's own parser, assemble the device and write it to `devices/`. |
| `dump <file> <out.txt>` | Decode any `.bwdevice` or `.bwmodulator` into a readable tree of classes (`C…`) and properties (`P…`). |
| `nitcheck <file.nit>` | Syntax-check Nitro source and print what it decodes back to. |

## Adding a device

1. Copy `src/tremolo-plus/` to `src/<name>/`.
2. Write the DSP in `<name>.nit` and check it with `nitcheck`.
3. Rename and adapt the builder class (it extends `DocKit`, which has helpers for atoms, ports, connections, widgets and writing). Keep the main class name unique, because all builders compile into one default package.
4. Update `device.properties` (`main`, `nit`, `output`, `templates`).
5. Add a row to the device table in the README and a screenshot in `previews/`.

## Troubleshooting

- **`javac` or `jar` not found**: you have a JRE only; install a JDK and reopen the terminal.
- **`class file has wrong version`**: run `setup` again so `PatchClasses` runs with the JDK you are building with.
- **`NoClassDefFoundError: org/antlr/v4/...`**: `libs.jar` was not found or has no ANTLR runtime; check `LIBS_JAR`.
- **`ClassCastException` / `no prop N on C...` in a builder**: the template files do not match what the builder expects (for example a different version of Orbit). Use the versions the builder was written against, or dump both with `dump` and compare.
- **The device does not change in Bitwig**: remove the old instance and add the new file again, and restart Bitwig through Patchbay.
