# Bitwig Devices

Native Bitwig Studio devices, with the source and tooling that build them. Devices are plain `.bwdevice` files, so you can use the files directly or rebuild them from source.

> Layout modeled on [polarity/bitwig-devices](https://github.com/polarity/bitwig-devices).

## Install

Custom devices are not loaded by stock Bitwig. Polarity's collection uses **Patchbay Mod Loader** with its **Custom Nitro Registration** framework, and these devices are meant to be loaded the same way. Follow the install notes in [Patchbay's releases](https://github.com/shaboogen-dev/Patchbay) and pick the build that matches your Bitwig version (these devices were built with **6.1.7**).

1. Install Patchbay and enable **Custom Nitro Registration**, as described in its `INSTALL.md`.
2. Download a file from [`devices/`](devices) (**Download raw file**), or use **Code → Download ZIP**.
3. Close Bitwig. In Patchbay open **Tools → Device Folders**, add the folder that contains the `.bwdevice` files, and launch Bitwig from Patchbay.
4. Find the device by name in Bitwig's device browser.

## Devices

The device names below link to the built files. The Panel column links to a screenshot of each panel.

| Device | Type | What it does | Panel |
| --- | --- | --- | --- |
| [Tremolo+](devices/Tremolo+.bwdevice) | Audio FX | Tremolo with Bitwig's seven LFO shapes, a Rate knob paired with Bitwig's full timebase list (Hz, kHz, bar, straight/dotted/triplet notes, pitch of the current note, hold), depth, and a note-retrigger toggle. [Design notes](src/tremolo-plus/README.md) | [Preview](previews/tremolo-plus.png) |

## Modulators

None yet. Put `.bwmodulator` files in [`modulators/`](modulators).

## Grid modules

None yet. See [`grid-modules/`](grid-modules).

## Compatibility and updates

- Built against **Bitwig Studio 6.1.7** on Windows x64.
- **Tremolo+**: loads and shows its panel in Bitwig 6.1.7 on Windows 10 (author's machine). The icons, the vertical shape buttons, the R toggle and the dropdown were checked on screen. Audio behaviour of every shape and time base, tempo sync with the transport stopped, and project recall have not been verified yet.
- macOS, Linux and other Bitwig versions are untested.
- Replacing a device file does not update copies already inside a saved project. Remove the device and add it again to see a new build.
- Keep copies of the exact files your saved projects use outside the scanned folders.

## Building from source

Everything is built by the scripts in [`scripts/`](scripts) against **your own** Bitwig installation; this repository contains no Bitwig code or assets. See [docs/BUILDING.md](docs/BUILDING.md).

```powershell
# Windows PowerShell (JDK 21+ on PATH)
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\setup.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build.ps1 tremolo-plus
```

```bash
# bash
export BITWIG_DIR="/path/to/Bitwig Studio"
scripts/setup.sh && scripts/build.sh tremolo-plus
```

## Repository layout

| Path | Contents |
| --- | --- |
| `devices/` | Built `.bwdevice` files |
| `modulators/` | Built `.bwmodulator` files |
| `grid-modules/` | Grid module downloads and status |
| `previews/` | Panel screenshots used by the device table |
| `src/<device>/` | Nitro source, builder and `device.properties` for each device |
| `tools/src/` | Shared tools: file reader/writer, document helpers, dump, Nitro check |
| `scripts/` | `setup`, `build`, `dump` and `nitcheck` for PowerShell and bash |
| `docs/` | What is known about the file format, Nitro and panels |

## Heads-up

- This work uses Bitwig's own classes from your install to read and write device files. Check Bitwig's license terms before publishing or sharing anything built this way.
- The Tremolo+ panel embeds the seven LFO shape icons taken from Bitwig's icon set. If you publish this repository, consider replacing them with your own artwork.
- Building needs template devices from other authors (see [docs/BUILDING.md](docs/BUILDING.md)). They are not stored here; download them yourself and respect their licenses.
