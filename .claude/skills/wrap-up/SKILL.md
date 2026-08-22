---
name: wrap-up
description: Close out a chunk of work on this repo - verify tests pass, log what changed and why in docs/BACKEND_PLAN.md, and commit. Use when the user asks to wrap up, finish here, log progress, update the plan with what was done, or otherwise close out the current session's work.
---

Closes out a block of work on the CandyCorn shop backend the same way it's been done throughout this project: verify → document the *why* → commit. Invoking this skill is the user's explicit request to commit; don't ask for confirmation to commit itself, but do stop and surface anything that fails along the way instead of pushing through it.

## Steps

1. **Check the branch.** Run `git status --porcelain --branch` and `git branch -vv`. Confirm the current branch matches the feature actually being worked on (e.g. don't let order-module changes land on `feature/catalog` — see the branching note in `CLAUDE.md`, this repo already had to fix that once with a history rewrite). If it looks mismatched, stop and ask rather than committing to the wrong branch.

2. **Run the test suite first.** `./mvnw.cmd test` (or `./mvnw` on non-Windows). Do not write a "done" entry in the plan or commit if tests are failing — fix them first, or tell the user what's broken and stop.

3. **Figure out what actually changed.** Use `git status`/`git diff` plus what happened earlier in the conversation — don't just restate the diff mechanically. For each change, capture the *why* (a design decision, a bug that motivated it, a tradeoff chosen), not just a description of the code — that's the part that goes stale-proof in the plan and the part a diff alone doesn't tell you later.

4. **Add a dated entry to `docs/BACKEND_PLAN.md`.** Find the `## Progreso` section and add a new `### YYYY-MM-DD` subsection (today's date) if one doesn't already exist for today, in Spanish, matching the tone and bullet-list style of the existing entries. Keep it tight: what changed, why, and anything left pending for next time — skip anything derivable from reading the code (exact method signatures, file lists).

5. **Stage only what belongs in this commit.** Never `git add -A`/`git add .`. Review `git status` after staging to make sure nothing unexpected (stray temp files, unrelated edits) is included.

6. **Commit.** Follow this repo's convention: imperative one-line summary (`type: short summary`, English, matching `git log` style), a body explaining the *why* when it's not obvious from the diff, ending with:

   ```
   Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
   ```

7. **Report back briefly**: what got committed (hash + branch), and confirm tests were green. Don't push unless the user has already established that as expected for this session (e.g. earlier repo-wide docs went straight to `main`; feature branches generally don't get auto-pushed).
