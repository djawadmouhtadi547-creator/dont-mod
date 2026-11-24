# DON'T — THE GAME IS NOT OVER (djawad1)
Minecraft 1.20.1 · Fabric · Java 17

## Build
Push this folder to GitHub: Actions -> "build" produces `build/libs/dont-1.0.0.jar` (artifact `dont-mod`).
Locally: install Gradle 8.8 + JDK 17, then `gradle build`.

## Test commands (op)
/dont status | day <n> | appear | steps | glitch | animals | swarm | breach | exit | hunt | reset

Quick full test: new world -> /dont day 12 -> /dont breach -> walk to the door or /dont exit -> final hunt -> fake crash.
Reset: /dont reset (also removes the "dont" world name).

## Config: config/dont.json
## Custom assets: sounds (final_jumpscare, glitch), textures (jumpscare, glitch, black_entity), data/dont/structures/maze.nbt
## Tools: tools/lit2nbt.py (litematic -> structure NBT), tools/points.py (start/exit points)
