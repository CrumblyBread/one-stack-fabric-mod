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
| `/stack list add <item>` | **Op only.** Adds an item to the list and saves `items.json` immediately. Takes effect for everyone right away, no restart needed. |
| `/stack list remove <item>` | **Op only.** Removes an item from the list and saves `items.json` immediately (also clears its completion status). No restart needed. |
| `/stack list reload` | **Op only.** Re-reads `items.json` from disk, picking up manual edits without restarting the server. |

Items that are not in the list are rejected with `not on the list`. The `add`/`remove`/`reload` subcommands require op (permission level 2, "gamemaster") and can also be run from the server console.

## Config

Created on first server start under `config/one_stack/`:

- **`items.json`** — JSON array of item IDs that count for the challenge. Use `/stack list add`/`/stack list remove` to edit it live, or hand-edit the file and run `/stack list reload` (both work without restarting the server).
- **`progress.json`** — server-wide completed item IDs.

The default `items.json` is generated from the game registry: anything with max stack size greater than 1, minus a small denylist of survival-unobtainable items (bedrock, barriers, command blocks, spawn eggs, etc.). Order follows the creative inventory, so a wood set stays together and wool, carpets, concrete, and stained glass stay grouped by color. Items that are not in a creative category tab are appended alphabetically. Delete `items.json` and restart the server (or run `/stack list reload`) to regenerate it. For other Minecraft versions, retarget the mod build and replace/regenerate that list.

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
