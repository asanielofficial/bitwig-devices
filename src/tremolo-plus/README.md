# Tremolo+

Audio FX. Source: [`tremolo_plus.nit`](tremolo_plus.nit) (DSP) and [`TremoloPlusBuild.java`](TremoloPlusBuild.java) (device document, panel and wiring). Build with `scripts/build.sh tremolo-plus` or `scripts\build.ps1 tremolo-plus`.

## Controls

| Control | Does |
| --- | --- |
| Shape (7 buttons) | Bitwig's LFO shapes in Bitwig's order: sine, triangle, square, ramp up, ramp down, sample and hold, sample and slide (cubic) |
| R (toggle) | Restart the LFO on every note-on |
| Rate (knob) | Multiplier from 0.02 to 50 with 1.00 at the center, applied to the selected time base |
| Time base (dropdown) | Hertz, Kilohertz, Bar, Half/Quarter/8th/16th/32nd note, dotted and triplet versions of each, Pitch (of current note), Hold (rate of zero) |
| Depth (knob) | 0 = no effect, 1 = full tremolo |

## Behaviour notes

- Hertz and Kilohertz: frequency = rate multiplier in Hz or kHz.
- Note values and Bar: cycle length = note length divided by the rate multiplier, driven by the transport's `beats_per_frame`. Bar assumes 4 beats (no time-signature input).
- Pitch: frequency of the last note-on, scaled by the rate multiplier.
- Hold: the LFO stops at its current phase.
- Sample and hold / sample and slide use a small internal random generator; they step or interpolate once per cycle.

## Status

The shape, time base and retrigger ports were corrected after finding that Bitwig passes enum values as integers and the toggle as a boolean (`value_inport i32` / `bool`). See [docs/NITRO.md](../../docs/NITRO.md).
