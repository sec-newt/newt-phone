# What to look for when reviewing Newt Phone

Read [CLAUDE.md](CLAUDE.md) first; flag code that breaks a rule there, and lines in it
that have gone stale.

## What "broken" means here

In order of how bad it is:

1. **A call goes wrong.** An emergency number blocked, delayed or screened; a real caller
   silenced or blocked; a call placed to the wrong number; an incoming call that never
   shows.
2. **It leaks.** A new permission, anything that could send contacts, numbers or call
   history off the phone, numbers in logs, or an exported component or permission other
   apps can reach (the shared block rules are signature-only).
3. **The owner can't use it.** Text under 16sp or under 7:1 contrast, buttons under 56dp
   or rows under 48dp, an icon with no words, something missing for TalkBack (no label, or
   two things reading out the same), a layout that breaks at 200% text, or a feature that
   needs a swipe or typing with no button.
4. **The app breaks.** A crash, a hang on the main thread, or a lost block list, favorite
   or setting. A crash on the call screen means a missed call.
5. **It lies.** README, a label, a hint or a status that says something different from
   what the code does.

## Always call out, even if it looks fine

Any change to `.github/workflows/`, `scripts/`, `config/allowed-permissions.txt`,
`AndroidManifest.xml`, signing, release, or `gradle/libs.versions.toml`. Say in
one line what changed so the owner reads it themselves.

## How to report

- Evidence for every finding: file and line, and what goes wrong for the person using it.
  Don't guess from a name.
- Tag each one **Important**, **Nit** or **Pre-existing** (a problem this PR didn't add).
- At most 3 nits. Skip style, naming and formatting unless it hurts readability.
- Start the summary with a count: "2 important, 1 nit." If nothing needs fixing, say so
  plainly: "Nothing to fix."
- Don't repeat a finding that's already posted on this PR.

## Skip

`app/build/`, `build/`, `docs/superpowers/` (old plans and specs), and screenshot images.
