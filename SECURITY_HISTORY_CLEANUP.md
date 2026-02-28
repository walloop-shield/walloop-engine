# Security History Cleanup (BFG)

This runbook removes sensitive artifacts from Git history before making the repository public.

## 1) Create a mirror clone

```bash
git clone --mirror <REMOTE_URL> walloop-engine.git
cd walloop-engine.git
```

## 2) Remove tracked certificate path from history

```bash
java -jar bfg.jar --delete-files neon-root-ca.pem
```

## 3) Replace leaked secret strings from history

Copy `bfg-replacements.txt` from the working repository root into this mirror clone directory and run:

```bash
java -jar bfg.jar --replace-text bfg-replacements.txt
```

## 4) Cleanup unreachable objects

```bash
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

## 5) Verify

```bash
git log --all -- '**/neon-root-ca.pem'
git grep -n "***REMOVED***" $(git rev-list --all)
```

## 6) Push rewritten history

```bash
git push --force --all
git push --force --tags
```

## 7) After force push

- Invalidate old clones and ask the team to reclone.
- Rotate all secrets that appeared in history, even after rewrite.
- Confirm CI/CD and Fly deploy tokens are healthy.
