# `//` Alias Spike + Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the new tools WorldEdit-style `//` aliases — `//roof`, `//loft`, `//revolve`, `//resize`, `//shadow`, `//smear`, `//fc` — so builders switching from Arceon keep their muscle memory — spec §11, Phase 3 feature 5. `/gz*` stays canonical.

**Architecture (spike result):** FAWE 2.16 / WE 7.3+ register commands with Piston. Path: `WorldEdit.getInstance().getPlatformManager().getPlatformCommandManager().getCommandManager()` (a `org.enginehub.piston.CommandManager`); `register(name, builder -> ...)` with a greedy optional argument (`CommandParts.arg(...).variable(true).defaultsTo(List.of())`), a `condition` checking `injectedValue("actor", Actor.class).hasPermission(...)`, and an `action` that adapts the WE player via `BukkitAdapter.adapt` and forwards `String.join(" ", params.valueOf(arg).asStrings())` to the existing command handler. `PlatformCommandManager` maps `//name` to piston command `name`, so registering `roof` makes `//roof` work. Helper `shared/worldedit/WeAliases.register(plugin, name, permission, handler)` wraps it, skips names already taken (`containsCommand`), and logs instead of crashing when WE/FAWE is absent (dependency is `required: false`). Each new module calls it in `enable()`: roof→`roof(...)`, loft→new `LoftCommand.dispatch` (subcommand word or build), revolve, resize, shadow, smear→`bind`, fix-connect→`fc`.

**Tech Stack / Spec:** Java 25, Paper 26.2, FAWE 2.16.1 (piston API verified against EngineHub javadocs 0.5.7 / FAWE 2.16.2), `mvn -B verify` = CI (no local JDK). Spec: `docs/arceon-parity-spec.md` §11.

## Global Constraints
No new permissions or lang keys (alias descriptions are plain Components, not catalogue templates). No new module. ArchUnit: helper lives in `shared/worldedit`, modules already may use shared. Runtime behaviour can't be unit-tested here (no server); CI compile + existing tests are the gate. README gets an aliases note. No new deps (piston ships inside FAWE).

## Tasks (all done in commits on branch vibe/arceon-parity-spec-88e6d4)
1. Plan + ledger, `WeAliases` + `LoftCommand.dispatch` — commit.
2. Wire aliases into the seven modules — commit.
3. README + push, CI green — commit.

## Review Focus
- `containsCommand` guard so we never shadow a WE/FAWE built-in; try/catch around registration because FAWE is an optional dependency.
- Condition uses the WE actor's permission, so `//roof` respects the same `gzmnbuildtools.roof` permission as `/gzroof`.
