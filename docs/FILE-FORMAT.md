# `.bwdevice` file format (Bitwig 6.1.x)

Everything here was read from Bitwig's own classes (`com.bitwig.base.serial`, `com.bitwig.flt.file.format`) and checked against real device files. Items marked *(inferred)* come from reading existing devices, not from code.

## Container

- Text header `BtWg` plus hex fields, then a binary **meta** object (device name, id, creator, category, tags, version, revision, note/audio input flags), then padding, then the **contents** object.
- Meta keys seen: `application_version_name`, `device_name`, `device_id`, `device_uuid`, `device_type` (`audio_to_audio`), `device_category`, `device_description`, `creator`, `device_creator`, `tags`, `has_audio_input`, `has_audio_output`, `has_note_input`, `has_note_output`, `preset_name`, `revision_id`, `revision_no`, `type` (`application/bitwig-device`).
- The `device_id` / `device_uuid` must match the contents' UUID (property 385).
- The reader and writer are `FloatDeviceContentsFileFormat`; the tools call them directly, so headers and padding are produced by Bitwig itself.

## Value stream

A contents object is a sequence of `(property id, value)` pairs ended by id 0. Each value carries a type tag. The value types, as named by Bitwig's reader, are: byte, int16, int32, int64, bool, float, double, string, string array, colour (4 floats), UUID, object, object reference, relative reference, external reference, null, object list, byte/int16/int32/int64/float/double/bool arrays and a string-to-object map. The numeric tag bytes are an implementation detail that Bitwig's classes handle; the tools never write them by hand.

- The writer stores integers in the **smallest type that fits**, so a logical int32 may read back as byte. Reading and writing through Bitwig's classes round-trips exactly, so the tools copy types as read.
- Object lists carry no per-item type tag for objects: each item starts with its class id, and a small reserved value ends the list *(inferred from the reader)*.
- The document format version written by the 6.1.7 classes is 209; Polarity's 6.1.3 files carry 207.

## References

- An `object_ref` is the **1-based position of the target object in write order**, counting every object (root = 1, including embedded colour/base objects).
- The first place an object is written owns it; later uses are references. Bitwig expects references to point **backwards**, so the writer must emit owners first. This is why `P177` (input proxies) is written before `P173` (child components) in devices that reference those proxies from a Nitro module.
- `DocKit.resolve` converts ids to object pointers after reading; `DocKit.renumber` assigns ids again before writing and `DocKit.write` rejects forward references.

## Classes and properties used by the devices here

Ids are the numbers Bitwig stores in files.

### Device root (class 151)

| Property | Meaning |
| --- | --- |
| 177 | input proxies: audio proxy (owned here), note proxy |
| 173 | child components (atoms, Nitro modules, transport) |
| 6213 | panel pages |
| 178 | output proxies (class 50 = audio out, one connection from the Nitro output) |
| 385, 386, 2057, 387, 389, 390 | uuid, name, description, creator, tags, category |

### Components

| Class | What | Notes |
| --- | --- | --- |
| 2000 | audio source atom ("In") | its connection source is the audio input proxy (class 154) |
| 1371 | delay compensation | output 0 feeds the Nitro `in` |
| 1839 | transport | outputs: 0 playing, 1 `beat_time`, 2 `beats_per_frame` |
| 289 | decimal parameter | 701 id string, 374 name, 821 short name, 702 value type (class 123: 891 default), 712 value |
| 180 | enum parameter | 457 selected index; value type class 155 with items (393) |
| 258 | enum item | 650 index, 651 name, 9823 **icon**, 9824 description, 10795 deprecated |
| 87 | boolean parameter | value type class 198, default in 6957 |
| 1721 | Nitro module | 10421 compiled code, 15756 input ports, 15757 output ports, connections in base object 6194 → 614 |
| 2428 | Nitro port record | 8557 name, 8559 width, 8560 index, 8561 class (1 audio, 2 event, 3 value), 8562 is-input, 8565 event kind, 8566 type string |
| 105 | connection | 248 source object (reference), 249 source port index, 6443 double |

A connection list has one entry per Nitro input, in port order.

### Panel

| Class | Widget | Properties |
| --- | --- | --- |
| 1680 | page | 6211 name, 6212 content (class 1681, widget list 6221), 6232 width, 6233 height |
| 1694 | layout rectangle (every widget, property 6226) | 6215 x, 6216 y, 6217 width, 6218 height |
| 1687 | knob | 6220 bound parameter |
| 1712 | button group chooser | 6255 horizontal, 6256 break count, 6257 label style, 7548 button style, 10143 colour |
| 1714 | popup (dropdown) chooser | 6262 label style, 6266 style, 7528 colour |
| 1702 | toggle button | 6241 label text, 6240 style, 6239 icon name, 6267 label style, 6552 invert, 6719 selected |
| 1719 | image | 6259 SVG text, 6400/6401 alignment, 6767/6768 scale |
| 1683 | text label | 6219 text |
| 5512 / 5517 | shader drawing and its widget | 16316 SkSL source |

Common widget properties: 6220 bound parameter (reference), 6867 tooltip, 6309/6310 flags. Label style values: 0 text, 1 icon, 2 both.
