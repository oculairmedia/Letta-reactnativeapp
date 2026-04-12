# Agent Instructions

This project uses **bd** (beads) for issue tracking. Run `bd prime` for full workflow context.

## Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work atomically
bd close <id>         # Complete work
bd dolt push          # Push beads data to remote
```

## Non-Interactive Shell Commands

**ALWAYS use non-interactive flags** with file operations to avoid hanging on confirmation prompts.

Shell commands like `cp`, `mv`, and `rm` may be aliased to include `-i` (interactive) mode on some systems, causing the agent to hang indefinitely waiting for y/n input.

**Use these forms instead:**

```bash
# Force overwrite without prompting
cp -f source dest           # NOT: cp source dest
mv -f source dest           # NOT: mv source dest
rm -f file                  # NOT: rm file

# For recursive operations
rm -rf directory            # NOT: rm -r directory
cp -rf source dest          # NOT: cp -r source dest
```

**Other commands that may prompt:**

- `scp` - use `-o BatchMode=yes` for non-interactive
- `ssh` - use `-o BatchMode=yes` to fail instead of prompting
- `apt-get` - use `-y` flag
- `brew` - use `HOMEBREW_NO_AUTO_UPDATE=1` env var

## Material Design System Rules

Use these rules for all new UI work and UI refactors in this repo. The app already uses Material 3 broadly; the goal is to use it **systematically**.

### 1. Stable chrome, expressive content

- Treat app bars, drawers, and other navigation chrome as **stable structure**.
- Put rich emphasis, hero moments, and shared transitions on **content surfaces** like cards, rows, tiles, and detail headers.
- Avoid anchoring critical shared-element transitions inside unstable collapsing chrome unless there is no safer option.

### 2. Fix geometry before tuning motion

- If an animation glitches, first fix layout/state restoration and anchor stability.
- Do **not** try to solve geometry bugs with spring tuning, expressive motion, or shape morphing alone.
- Shared-element transitions must use deterministic source and destination bounds before any motion polish is added.

### 3. Prefer reusable screen shells over ad hoc screen composition

- Search/list/admin screens should converge on a standard shell:
  - `Scaffold`
  - `LargeFlexibleTopAppBar` (or a simpler top bar when hierarchy does not need a flexible header)
  - optional search slot
  - optional selection/filter row
  - optional FAB slot
  - standardized loading / error / empty / content states
- Reuse existing design-system components instead of rebuilding screen structure per feature.

### 4. Use Material components by interaction semantics

- Use **segmented buttons** or another mutually-exclusive selector pattern for binary/mode switches when only one option can be active.
- Use **chips** for filters, tags, or multi-select style controls.
- Use **bottom sheets** / `ActionSheet` for contextual and destructive action menus.
- Use **dialogs** / `ConfirmDialog` for confirmation and blocking decisions.
- Use **cards** and **surfaces** to communicate emphasis and hierarchy, not arbitrary decoration.

### 5. Motion policy

- Keep one motion language across the app: low-surprise navigation, stable shared elements, and small expressive accents inside content.
- The app theme already uses `MaterialExpressiveTheme` with `MotionScheme.expressive()`, but navigation should still feel conservative and readable.
- Fix geometry and state restoration before tuning motion. If a transition is glitchy, first verify stable anchors, scroll state, and consistent source/destination layout.

#### Navigation transitions

- Prefer the centralized drill transition pattern in `android-compose/app/src/main/java/com/letta/mobile/ui/navigation/AppNavGraph.kt` for drill-in screens.
- Keep navigation transitions brief, directional, and consistent. Use the shared drill timing/constants instead of inventing per-screen durations or easing.
- Do not add one-off route animations unless the navigation model is materially different and the exception is documented in the owning screen or issue.
- When a screen does not need special treatment, prefer default navigation behavior over custom animation.

#### Shared-element transitions

- Shared elements must go through `optionalSharedElement()` so they degrade safely when no shared scopes are present.
- Only use shared-element motion for stable content-to-content relationships such as list/grid items to detail headers.
- Never anchor critical shared elements inside unstable or collapsing chrome such as `LargeFlexibleTopAppBar`, collapsing search bars, or transient toolbar content.
- Prefer destination anchors in stable content surfaces like cards, rows, tiles, or fixed detail headers.
- Shared-element keys must be deterministic and unique to the item identity.
- If either side cannot provide stable bounds, remove the shared element and fall back to standard navigation motion.

#### Sheets, drawers, and contextual surfaces

- Use Material components such as `ModalBottomSheet`, `ActionSheet`, `ModalNavigationDrawer`, and dialogs for container motion instead of hand-rolled enter/exit animation.
- Let container components own their default motion unless a reusable design-system wrapper needs a documented adjustment.
- Contextual actions should appear from sheets or menus, not from bespoke animated overlays.

#### Micro-interactions

- Use expressive motion for local content changes only after layout geometry is correct.
- Preferred patterns are small, reusable helpers like `AnimatedVisibility`, `animateContentSize`, and icon rotation for expand/collapse, banners, and inline affordances.
- Keep micro-motion close to the component that owns the state change; avoid animating entire screens for small local updates.
- Avoid stacking multiple animations on the same interaction unless they clearly reinforce hierarchy.

#### Lazy lists, loading, and progressive data

- Do not use motion to hide expensive loading or unstable list geometry.
- For list/detail flows, preserve stable item identity and restore list/grid/top-app-bar state before adding polish.
- Prefer progressive rendering when secondary data arrives later. Show primary content first and add lightweight status affordances, such as the MCP loading banner, instead of blocking the full screen.
- Loading motion should communicate status, not delay access. Use existing shimmer/progress patterns and remove them as soon as content is ready.

#### Decision rules

- If motion clarifies spatial relationship or state change, keep it.
- If motion competes with readability, causes layout jumps, or depends on fragile anchors, simplify or remove it.
- Reuse existing repo patterns before adding a new motion treatment. New reusable motion behavior belongs in `android-compose/designsystem` or shared navigation helpers, not inside a single screen.

### 6. Surface and hierarchy rules

- Follow the repo theme layer and `LettaTopBarDefaults` rather than inventing per-screen app-bar/surface colors.
- Use consistent corner-radius and surface emphasis rules across similar cards and tiles.
- Prefer a clear surface hierarchy over stacking multiple decorative treatments on the same element.

### 7. Typography rules

- Use the existing repo typography tokens and extensions (`MaterialTheme.typography`, `listItemHeadline`, `listItemSupporting`, etc.) as the default.
- Prefer semantic extensions from `TypeHierarchy.kt` such as `sectionTitle`, `chatBubbleSender`, and `statValue` over one-off styling in screens.
- Do not add raw `fontWeight` overrides in feature UI when an existing typography token can express the hierarchy. Add or extend a semantic token first, then reuse it.
- Introduce expressive or emphasized typography only for real hierarchy changes or hero moments, not as ad hoc styling.

### 8. Prefer in-repo patterns first

- Before inventing a new UI pattern, search for an existing implementation in the repo and reuse/adapt it.
- The `android-compose/designsystem` module is the canonical home for reusable UI foundations. Add new shared components there rather than creating per-screen one-offs.
- Current reusable foundations include `ActionSheet`, `ConfirmDialog`, `EmptyState`, `ErrorContent`, themed top-bar defaults, and the shared design-system theme layer.

### 9. Design-system direction issues

- `letta-mobile-q7jt` — Establish Material design system guardrails
- `letta-mobile-iqwq` — Build reusable searchable list screen scaffold
- `letta-mobile-pkzn` — Define motion and navigation transition policy
- `letta-mobile-ns9n` — Standardize selection and emphasis components

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->

## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**

- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->
