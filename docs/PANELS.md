# Panels

## Size

- A panel page is `width x height` grid units. **Every device's height is 53** and the device area is fixed, so a page taller than 53 clips and a shorter one leaves empty space at the bottom. Width is free. One unit is about 6 px at normal zoom.
- Widgets sit on this grid with `x, y, width, height` (class 1694). Use equal margins and a regular rhythm; Tremolo+ uses a 36 x 53 page, 3-unit margins, two 14-wide columns with a 2-unit gap and 1-unit gaps between stacked controls.

## Icons

- **Enum items** (dropdown rows, button labels): property 9823 holds the icon as **`<name>.svg`**, for example `unit-hz.svg`, `note-16.svg`, `tune.svg`. Tested forms: `name.svg` works; `name`, `device/name`, `device/name.svg`, `icons/device/name.svg` and a leading slash did not show icons. Icons come from Bitwig's `resources/icons/device/`.
- **Image widget** (class 1719): takes SVG text in property 6259. Polarity's devices place one on top of a button with the same rectangle; the button stays clickable. Tremolo+ uses this for the shape buttons, because the button group does not draw item icons itself in this setup. Stroke icons need their stroke colour changed from `#000000` to something light.
- **Label style** (`0` text, `1` icon, `2` both) applies to the dropdown preview as well as the button group.

## Widgets

- Button group (1712): set `6255` false for a vertical stack; item heights are equal, so use a widget height that divides by the number of items, or round the overlay rectangles.
- Toggle (1702): label text is property 6241, bound to a boolean parameter (class 87).
- Dropdown (1714): label style 1 shows only the icon in the closed state.

## Findings from ASD Vibrato

- **Horizontal button groups have a fixed row height of about 5 units** (roughly 29 px) regardless of the rectangle you give them; the buttons stay at the top of the rectangle. A vertical group, in contrast, divides the rectangle's height between its buttons. Size horizontal groups and any icon overlays to 5 units tall.
- **A shader drawing needs its first input connected.** A drawing (class 5512) with all inputs disconnected rendered as an empty area. The blueprint's rule applies: feed input 0 from a Nitro `audio_outport f32<4>` output (a four-lane vector) and declare `uniform float4 audioLow;` in the shader. More inputs can be used the same way (`audioMedium` for input 1, as Orbit does); ASD Vibrato feeds two and leaves seven disconnected. Place the drawing after the Nitro module in the child list so its reference points backwards.
- **Only `iResolution` is provided.** Every reference shader defines its own helpers, including `coverage`: `float coverage(float distance, float pixel) { return 1. - smoothstep(-pixel * .65, pixel * .65, distance); }`. Calling `coverage` without defining it left the ASD Vibrato curve blank in the first two builds, because the shader did not compile. A shader that fails to compile shows nothing. Map `fragCoord / iResolution.xy` into a fixed design space, and stick to the built-ins the reference shaders use (`abs atan clamp cos dot exp2 floor fract length max min mix mod pow sin smoothstep step`).
