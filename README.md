# Evaluation Playground

This repository is used to evaluate coding agents against a human reference implementation.

The workflow is intentionally simple:
- `main` contains the base code
- `human` contains the human-written solution (ground truth)
- `agent` contains the agent-generated solution

Both human and agent changes are evaluated **against the same base code**.

## How evaluation works

1. Start from the `main` branch (base code)
2. Apply a human fix on the `human` branch and commit it
3. Apply an agent fix on the `agent` branch and commit it
4. Run `evaluate.sh` on each branch using the same base commit
5. Compare test results and security scan outputs

The focus is on functional coverage, execution behavior, and security impact — not exact code similarity.

## Running evaluations

All evaluations are run inside Docker to keep the environment consistent.

```bash
docker compose up -d --build
docker compose exec app bash
