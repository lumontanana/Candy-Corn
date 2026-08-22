---
name: start-of-day
description: Orient at the start of a session on the CandyCorn shop backend - sync branch state with origin, confirm a green test baseline, and recap where the plan was left. Use when the user says things like starting for the day, catch me up, what's the state, seguimos, or otherwise wants a status check before diving into new work.
---

Counterpart to `/wrap-up`: instead of closing out work, this orients at the *start* of a session, since state has repeatedly drifted between sessions on this repo (commits pushed from the IDE outside the conversation, PRs merged without Claude knowing, the repo itself getting renamed on GitHub). This is a read-only reconnaissance pass — report findings, don't act on them (no pulling/rebasing/committing) unless the user asks after seeing the briefing.

## Steps

1. **Fetch, don't assume.** `git fetch origin --prune`. Never trust that local state matches origin without checking first — this repo has already surprised sessions with commits pushed from the IDE and a PR merged mid-conversation.

2. **Check the working tree and current branch.** `git status --porcelain --branch`. Note any uncommitted changes as-is — they may be the user's in-progress work, don't touch them.

3. **Compare current branch vs its origin counterpart.** Ahead/behind counts (`git rev-list --left-right --count origin/<branch>...<branch>`). If behind, don't pull automatically — report it and let the user decide, the same way a force-push or reset would need confirmation.

4. **Check whether `main` has moved.** `git log --oneline main..origin/main` (after fetching) and look for merge commits (`Merge pull request #N from ...`) — that's how a PR merge showed up unannounced last time. If the current feature branch's base is now behind `main`, mention it; don't rebase/merge without being asked.

5. **Confirm a green baseline.** Run `./mvnw.cmd test` (or `./mvnw` outside Windows) once. If something is already broken before any new work starts, that needs to be flagged immediately, not discovered later mixed in with new changes.

6. **Recap the plan.** Read `docs/BACKEND_PLAN.md`: the most recent dated entry under `## Progreso` (what was last done and why) and `## Orden de implementación` (which step is current, what's next). Cross-check against what's actually in the code/branches rather than trusting the doc blindly if they've diverged.

7. **Give a short briefing, then stop.** Summarize: branch + sync status, test baseline (green or what's broken), one-line recap of where the plan left off, and the natural next step per the plan order — then wait for direction. This skill orients; it doesn't start implementing.
