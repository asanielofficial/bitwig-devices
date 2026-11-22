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
