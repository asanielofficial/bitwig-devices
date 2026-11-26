# ASD Vibrato

Audio FX (category Modulation). Source: [`asd_vibrato.nit`](asd_vibrato.nit) (DSP) and [`AsdVibratoBuild.java`](AsdVibratoBuild.java) (device document, panel and wiring). Design: [`ASD Vibrato.mockup.json`](ASD%20Vibrato.mockup.json) (open it with *Import JSON* in the Bitwig Device Builder) and [`previews/asd-vibrato-mockup.png`](../../previews/asd-vibrato-mockup.png) (the panel as built: [`previews/asd-vibrato.png`](../../previews/asd-vibrato.png)). Build with `scripts/build.sh asd-vibrato` or `scripts\build.ps1 asd-vibrato`.

## Controls

| Control | Does |
| --- | --- |
| Curve display | Live: the pitch curve of the selected shape, scaled by Depth (full height = 100 cents), a lighter curve for the right channel when Stereo is above zero, and one LFO cycle across the width on a black backdrop, with a cursor at the current LFO phase that starts at the far left and sweeps right. Sample and hold / slide show the actual held values |
| Shape (7 buttons) | Sine, triangle, square, ramp up, ramp down, sample and hold, sample and slide |
| Rate (knob) | Multiplier from 0.02 to 50 with 1.00 at the center, applied to the selected time base. **On Hold it becomes a position control**: the knob sets the LFO position from 0 to one full cycle (smoothed over 30 ms), and the cursor in the curve display follows it |
| Time base (dropdown) | Hertz, Kilohertz, Bar, straight/dotted/triplet notes, Pitch (of current note), Hold (the LFO stops and Rate scrubs its position) |
| R (toggle) | Restart the LFO on every note-on |
| Depth (knob) | Peak pitch deviation of a sine, 0 to 100 cents |
| Stereo (knob) | Phase offset of the right channel's LFO, 0 to half a cycle |
| Mix (knob) | Dry/wet, 100% by default |

## How it works

A stereo delay line (a tenth of a second, cubic interpolation) is read at a delay that follows the LFO. A delay that changes at a rate *r* shifts the pitch by *r*, so for the **pitch** to follow a shape *x(φ)*, the delay has to follow the integral of that shape. The device uses the exact zero-mean integral of each shape:

| Shape | Delay follows |
| --- | --- |
| Sine | cosine |
| Triangle | piecewise parabola |
| Square | triangle (pitch trills between two values, no clicks) |
| Ramp up / down | parabola (pitch sweeps, then snaps back) |
| Sample & hold, sample & slide | a running integral of the held values, kept within bounds by choosing each new value so the running sum stays near zero |

The delay swing is `kappa x (peak of the integral)` with `kappa = (2^(cents/1200) - 1) / cycles per frame`, limited to 30 ms and smoothed over 20 ms, so very slow rates with large depth cannot reach the full swing (random shapes reach about a third of it at 0.5 Hz and 100 cents).

- The base delay follows the swing, so the wet signal is delayed by about half the peak swing: a few milliseconds at typical vibrato settings, up to 30 ms at very slow, deep settings.
- In Hold, the delay is a function of the knob position, so moving Rate moves the vibrato like a pitch wheel. The swing is sized as if the LFO ran at 1 Hz: Depth is the pitch swing you get when sweeping the position at one cycle per second, and slower or faster sweeps scale it. Retrigger is ignored while Hold is selected.
- Retriggering resets the phase, which makes the delay jump to the shape's value at phase 0 once.
- Stereo and Mix are smoothed over about 30 ms and the swing over about 80 ms. Knob values arrive in steps; without smoothing, a step in the Stereo offset jumps the right channel's delay and a step in Mix jumps the blend, which buzzes while a knob is moved.
- The right channel uses the same shape delayed by the Stereo offset (and, for the random shapes, the same held values).
- Checked numerically (a Python port of the delay law): at 5 Hz and 50 cents every shape produced exactly 50 cents of pitch swing with no mismatch to its curve.

## Visualizer

The Nitro module outputs two four-value vectors that feed the shader drawing: `display` (shape index, depth, LFO phase, stereo offset in cycles) and `display_hold` (the held random values: two segments ago, previous, current). The shader (`CURVE_SHADER` in the builder) shows one cycle: for the random shapes the current held segment, with the previous one visible in the lagged right-channel curve.

## Status

The DSP of the first build was checked in Bitwig by the author and works. The later builds changed the shapes (pitch follows the shape) and made the curve display live; the display stayed blank because the shader called an undefined `coverage` function, fixed in the current build. Smoothing for Stereo and Mix was added after buzzing was reported. The curve display has been seen working in Bitwig (screenshot in `previews/asd-vibrato.png`). The latest DSP changes have not been confirmed by ear yet. Not implemented from the mockup: the link that greys out Rate while "Hold" is selected.
