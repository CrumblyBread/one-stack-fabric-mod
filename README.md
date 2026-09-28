# One Stack

Server-only Fabric mod for a shared challenge: collect one full stack of every obtainable item.

## Requirements

- Minecraft **26.3**
- Fabric Loader **0.19.5+**
- Fabric API
- Java **25** (for building)

Install the mod jar in the server's `mods` folder. Clients do not need the mod.

## Commands

| Command | Description |
|---|---|
| `/stack` | If your main hand holds a full stack of a listed item, marks it complete for the whole server and removes that stack. |
| `/stack list` | Opens a double-chest GUI of every listed item on page 1. Incomplete = 1, completed = full stack. Bottom row: red concrete named Previous, gray concrete stacked to the page number, green concrete named Next. |
| `/stack list <page>` | Opens that checklist page (1-based, same number the gray concrete shows). Previous and Next still move from there. |

Items that are not in the list are rejected with `not on the list`.

## Config

Created on first server start under `config/one_stack/`:

- **`items.json`** — JSON array of item IDs that count for the challenge. Edit freely (add/remove/reorder), then restart the server.
- **`progress.json`** — server-wide completed item IDs.

The default `items.json` is generated from the game registry: anything with max stack size greater than 1, minus a small denylist of survival-unobtainable items (bedrock, barriers, command blocks, spawn eggs, etc.). Order follows the creative inventory, so a wood set stays together and wool, carpets, concrete, and stained glass stay grouped by color. Items that are not in a creative category tab are appended alphabetically. Delete `items.json` and restart the server to regenerate it. For other Minecraft versions, retarget the mod build and replace/regenerate that list.

## Build

A local JDK 25 is under `.jdk/` (gitignored). Dependencies are cached by Gradle after the first download.

```bash
./build.sh build
```

Or with a system JDK 25:

```bash
./gradlew build
```

The jar is written to `build/libs/`.
