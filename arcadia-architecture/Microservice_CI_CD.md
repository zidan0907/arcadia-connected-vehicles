# Microservice CI/CD & Release Governance

> GitHub Actions pipeline, branch governance, Docker image isolation, and Flyway migration workflow for Java 21 microservices.

## Overview

- `develop` is the protected integration branch.
- All development takes place on `feature/*` branches.
- Pull requests into `develop` require at least **2 peer approvals** and passing required checks.
- CI runs tests, SonarQube analysis, compilation, and packaging automatically.
- Docker image publishing is gated by explicit approval.
- PostgreSQL schema changes are managed with versioned Flyway migrations.

## 1. Branching Strategy & PR Governance

### Protected integration branch: `develop`

- The single integration target for validated code.
- Used for end-to-end integration and flow testing.
- Direct pushes and force pushes should be disabled through repository branch protection/rulesets.
- Changes enter through pull requests.

### Feature branches: `feature/*`

Every active development branch must start with `feature/`.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/remote-ops-climate
```

Pull requests from branches that do not follow the prefix convention should fail branch-name validation.

### Merge requirements

- Minimum **2 peer approvals**.
- All required automated checks must pass.
- Test failures or a failed SonarQube quality gate block merging.
- Merge approved changes into `develop` for integration testing.

> Repository branch protection/rulesets must enforce approvals, required checks, and disabled direct/force pushes. A workflow file alone does not configure these repository-level settings.

## 2. Four-Stage CI/CD Pipeline

Workflow: `.github/workflows/microservice-ci.yml`

| Stage | Name | Trigger / Dependency | Core operations | Halt condition |
|---|---|---|---|---|
| **1** | Test & Quality Gate | Push or PR | Run JUnit 5 and Mockito tests; run SonarQube analysis and wait for its quality gate | Tests or quality gate fail. Configure an explicit coverage threshold if coverage drops must block CI. |
| **2** | Build | After Stage 1 | `mvn clean compile` | Compilation or dependency errors |
| **3** | Package | After Stage 2 | `mvn package -DskipTests`; build a local Docker image; archive the JAR | Packaging or Docker build failure |
| **4** | Deploy / Publish | After Stage 3; approval-gated environment | Authenticate to the registry and push the image | Approval is pending/declined, or registry authentication/push fails |

**Push safety rule:** Stages 1–3 run automatically. Stage 3 builds the image locally without pushing it. Stage 4 pushes the image only after the protected environment allows the job to proceed.

## 3. Docker Image Tags & Runtime Isolation

| Source branch | Tag format | Example |
|---|---|---|
| Feature branch | Sanitized branch name + `-SNAPSHOT` | `feature-remote-ops-climate-SNAPSHOT` |
| `develop` | `develop-` + commit SHA | `develop-7f3b89a...` |

The workflow replaces `/` in feature branch names with `-`.

### Network isolation

Services run on an internal Docker Compose bridge network:

- Containers communicate using internal service aliases.
- Database, message broker, and cache ports should remain internal.
- Do not expose infrastructure ports to the public host unless there is a clear operational need and appropriate access controls.

## 4. PostgreSQL Migrations with Flyway

Hibernate automatic schema updates (`ddl-auto=update`) are disabled. Database changes use versioned SQL migration scripts.

### Database isolation

Each microservice has its own database:

- `ops-db`
- `maint-db`
- `eco-db`

### Migration naming

```text
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__create_outbox_table.sql
└── V168__add_index_to_price.sql
```

Use unique, sequential migration versions. Avoid editing migrations already applied in shared environments; create a new migration instead.

### Dedicated migration workflow

Workflow: `.github/workflows/db-migration.yml`

It runs manually via `workflow_dispatch` or on pushes to `develop` that affect `src/main/resources/db/migration/**`.

The workflow:

1. Checks out the repository.
2. Sets up Java 21.
3. Runs `mvn flyway:info`.
4. Runs `mvn flyway:migrate`.

Connection details are supplied through GitHub Actions secrets.

> **Implementation note:** The workflow defines a `target_db` input (`all`, `ops-db`, `maint-db`, or `eco-db`), but the shown commands use one set of `FLYWAY_URL`, `FLYWAY_USER`, and `FLYWAY_PASSWORD` secrets. Add explicit target-to-connection mapping if each selection must migrate a different database.

## 5. GitHub Actions Workflows

### 5.1 Application pipeline

File: `.github/workflows/microservice-ci.yml`

Triggers on pushes to `feature/*` and `develop`, and pull requests targeting `develop`.

Key behavior:

- Cancels an older run when a newer run for the same workflow/ref starts.
- Validates the feature prefix on pull requests.
- Uses Temurin Java 21 and Maven caching.
- Runs tests and SonarQube analysis.
- Compiles and packages the application.
- Builds a Docker image without pushing in Stage 3.
- Uploads the JAR as the `app-jar` artifact for one day.
- Uses the `development-deploy-approval` environment for the gated publishing job.

| GitHub Actions secret | Purpose |
|---|---|
| `SONAR_TOKEN` | SonarQube authentication |
| `SONAR_HOST_URL` | SonarQube server URL |
| `CONTAINER_REGISTRY` | Container registry host |
| `REGISTRY_USERNAME` | Registry username |
| `REGISTRY_PASSWORD` | Registry password |

### 5.2 Database migration pipeline

File: `.github/workflows/db-migration.yml`

- Manual trigger through `workflow_dispatch`.
- Path-based trigger for migration changes pushed to `develop`.
- Uses Java 21 and runs Flyway info/migrate commands.
- Uses the `development-deploy-approval` environment.
- Reads connection settings from secrets.

| GitHub Actions secret | Purpose |
|---|---|
| `DEV_POSTGRES_JDBC_URL` | PostgreSQL JDBC URL |
| `DEV_POSTGRES_USER` | Migration database user |
| `DEV_POSTGRES_PASSWORD` | Migration database password |

## 6. Developer Operating Procedure

### Step 1 — Create a feature branch

```bash
git checkout develop
git pull origin develop
git checkout -b feature/remote-ops-climate
```

### Step 2 — Commit and push

```bash
git add .
git commit -m "feat: add remote operations climate support"
git push -u origin feature/remote-ops-climate
```

Stages 1–3 run automatically. The JAR is packaged and the Docker image is built locally in CI; no remote image is pushed yet.

### Step 3 — Publish a snapshot when needed

If teammates need the feature image for integration checks, approve the Stage 4 deployment action. Example tag:

```text
feature-remote-ops-climate-SNAPSHOT
```

### Step 4 — Add database migrations when needed

Create a new versioned SQL script under `src/main/resources/db/migration/`, for example:

```text
V169__add_remote_climate_status.sql
```

Run the migration workflow as appropriate and verify the migration status.

### Step 5 — Open a pull request

Open a PR from the feature branch into `develop`. Before merging, confirm:

- [ ] Branch name starts with `feature/`.
- [ ] Tests pass.
- [ ] SonarQube quality gate passes.
- [ ] Compilation and packaging pass.
- [ ] At least two peer reviewers approve.
- [ ] All repository-required checks pass.

Squash and merge into `develop` according to the repository's merge policy.

## 7. Operational Checklist

- [ ] `develop` is protected against direct pushes and force pushes.
- [ ] At least two PR approvals are required.
- [ ] Required CI status checks are configured.
- [ ] The deployment environment has the intended required reviewers.
- [ ] Secrets are stored in GitHub Actions, not committed to source control.
- [ ] Stage 3 builds the image without pushing.
- [ ] Stage 4 pushes only after approval.
- [ ] Database, broker, and cache ports are not exposed publicly.
- [ ] Hibernate DDL auto-update is disabled.
- [ ] Flyway migration versions are unique and ordered.
- [ ] The migration workflow maps each selected database to the correct connection settings.

## Summary

**Feature branch → Test & Quality Gate → Build → Package locally → Approval-gated publish → PR review → `develop` integration**

Database schema changes follow a dedicated Flyway migration workflow using versioned SQL scripts and credentials supplied through GitHub Actions secrets.
