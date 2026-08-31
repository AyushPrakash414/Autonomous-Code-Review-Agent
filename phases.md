# Autonomous Code Review Agent --- Implementation Phases

## Global Rule

Do not proceed to the next phase until the current phase objective is
objectively verified.

Every phase must have: - implementation; - tests; - failure tests; -
acceptance evidence; - documented result.

A green endpoint or UI is not sufficient evidence.

------------------------------------------------------------------------

# Phase 0 --- Project Foundation

## Objective

Create the repository and service boundaries.

## Deliverables

``` text
autonomous-code-review-agent/
├── spring-backend/
├── agent-service/
├── frontend/
├── infrastructure/
└── docs/
```

## Exit Criteria

-   Spring Boot starts.
-   FastAPI starts.
-   React starts.
-   Services have health endpoints.
-   Repository builds from a clean checkout.

------------------------------------------------------------------------

# Phase 1 --- Spring Boot Authentication

## Objective

Users can securely authenticate.

## Implement

-   Spring Security;
-   JWT;
-   password authentication;
-   Google OAuth2 if desired;
-   protected API.

## Tests

-   valid login;
-   invalid login;
-   expired token;
-   unauthorized endpoint;
-   authorized endpoint.

## Exit Criteria

A real user can authenticate and access a protected endpoint.

------------------------------------------------------------------------

# Phase 2 --- GitHub Connection

## Objective

Connect an authorized GitHub account/repository.

## Implement

-   GitHub authorization;
-   repository retrieval;
-   repository persistence.

## Tests

-   valid GitHub authorization;
-   repository listing;
-   unauthorized repository access;
-   token failure.

## Exit Criteria

A logged-in user can select an authorized repository from the dashboard.

------------------------------------------------------------------------

# Phase 3 --- GitHub Webhook

## Objective

A real GitHub PR triggers your system.

## Implement

-   webhook endpoint;
-   signature verification;
-   PR event parsing;
-   PR persistence;
-   review creation.

## Test Cases

1.  PR opened.
2.  PR synchronized.
3.  Invalid signature.
4.  Unknown repository.
5.  Duplicate webhook.

## Exit Criteria

Creating a real test PR creates exactly one corresponding review
revision.

------------------------------------------------------------------------

# Phase 4 --- Review Lifecycle

## Objective

Track a PR review from start to completion/failure.

## States

``` text
QUEUED
ANALYZING
ROUTING
REVIEWING
VERIFYING
RISK_ANALYSIS
COMPLETED
FAILED
```

## Exit Criteria

A review can transition through valid states and invalid transitions are
rejected.

------------------------------------------------------------------------

# Phase 5 --- PR Analyzer MVP

## Objective

Produce a real `PRAnalysisReport` from an actual PR.

## Implement

-   metadata;
-   Git diff;
-   changed files;
-   line counts;
-   file classification;
-   language detection.

## Test Fixtures

-   README-only;
-   Java source;
-   Java test;
-   YAML configuration;
-   multiple file PR;
-   rename;
-   deletion.

## Exit Criteria

Expected classification matches fixture truth.

------------------------------------------------------------------------

# Phase 6 --- PR Analyzer Intelligence

## Objective

Add AST, symbols, dependencies, tests, signals, and impact.

## Implement

-   AST analysis;
-   symbol changes;
-   dependency graph;
-   related-test discovery;
-   security signals;
-   architecture signals;
-   quality signals;
-   change impact.

## Exit Criteria

For controlled fixture repositories, the analyzer correctly
identifies: - changed symbols; - dependencies; - related tests; -
important signals; - impact level.

------------------------------------------------------------------------

# Phase 7 --- PR Analyzer Evaluation Suite

## Objective

Prove the analyzer is reliable before building agents.

## Required PR fixtures

### A

README-only.

### B

Architecture documentation.

### C

Controller feature.

### D

Service/business-logic feature.

### E

Authentication change.

### F

Repository/database change.

### G

Test-only change.

### H

Formatting-only change.

### I

Multi-module change.

### J

Ambiguous semantic change.

## Exit Criteria

The analyzer produces expected reports for every fixture.

Do not move forward if the analyzer only works on one example.

------------------------------------------------------------------------

# Phase 8 --- Agent Capability Registry

## Objective

Define what each agent can review.

## Agents

-   Security;
-   Architecture;
-   Test;
-   Code Quality.

## Exit Criteria

Each capability is represented in machine-readable configuration and has
unit tests.

------------------------------------------------------------------------

# Phase 9 --- Agent Router

## Objective

Produce an explainable `AgentExecutionPlan`.

## Implement

-   feature extraction;
-   capability matching;
-   weighted scoring;
-   thresholds;
-   confidence;
-   reasons;
-   conditional decisions.

## Required routing tests

README: Architecture only/conditional.

Authentication: Security + Architecture + Test + Quality.

Test-only: Test + Quality.

Formatting: minimal/no review.

Database change: Security + Architecture + Test + Quality.

## Exit Criteria

Routing test suite passes and every decision contains reasons.

------------------------------------------------------------------------

# Phase 10 --- LLM-Assisted Ambiguity Routing

## Objective

Use an LLM only when deterministic routing is ambiguous.

## Implement

-   LLM provider abstraction;
-   structured routing schema;
-   confidence;
-   validation;
-   fallback behavior.

## Test

Same ambiguous fixture with: - deterministic high confidence; -
ambiguous case; - LLM unavailable.

## Exit Criteria

LLM is not called for high-confidence deterministic decisions.

------------------------------------------------------------------------

# Phase 11 --- LangGraph Workflow

## Objective

Execute `AgentExecutionPlan`.

## Implement

-   review state;
-   selected-agent branches;
-   parallel execution where safe;
-   conditional branches;
-   retries;
-   failure handling.

## Exit Criteria

If router selects only Security and Test, LangGraph executes only those
agents.

------------------------------------------------------------------------

# Phase 12 --- Code Quality Agent

## Objective

Produce real maintainability findings.

## Implement

-   static analysis;
-   complexity;
-   duplication;
-   code smells;
-   repository conventions;
-   LLM explanation.

## Exit Criteria

Known quality defects in fixtures are detected and clean fixtures do not
produce fabricated findings.

------------------------------------------------------------------------

# Phase 13 --- Security Agent

## Objective

Detect and explain real security issues.

## Implement

-   static security scanning;
-   secret detection;
-   security patterns;
-   data-flow signals;
-   LLM reasoning.

## Exit Criteria

Known vulnerable fixtures are detected and safe fixtures are not falsely
marked as vulnerable without evidence.

------------------------------------------------------------------------

# Phase 14 --- Architecture Agent

## Objective

Review architecture against actual repository conventions.

## Implement

-   dependency graph;
-   layer detection;
-   architecture rules;
-   module boundaries;
-   architecture documentation context.

## Exit Criteria

Controlled layer violation is detected; valid architecture is accepted.

------------------------------------------------------------------------

# Phase 15 --- Test Agent

## Objective

Detect missing/weak tests and generate executable tests.

## Implement

-   test discovery;
-   test mapping;
-   coverage;
-   test generation;
-   test validation;
-   Docker execution;
-   test-quality evaluation.

## Exit Criteria

For a fixture with missing tests: 1. Agent detects missing coverage. 2.
Generates tests. 3. Tests compile. 4. Tests execute. 5. Results are
recorded. 6. Agent never claims success without execution.

------------------------------------------------------------------------

# Phase 16 --- Finding Merger

## Objective

Normalize and deduplicate agent findings.

## Exit Criteria

Multiple agents identifying the same root problem produce one correlated
finding with all evidence preserved.

------------------------------------------------------------------------

# Phase 17 --- Verification Agent

## Objective

Reduce false positives.

## Implement

-   evidence collection;
-   static confirmation;
-   repository context;
-   optional reproduction;
-   regression test.

## Exit Criteria

Known true positives become `VERIFIED`. Known false positives become
`FALSE_POSITIVE`. Ambiguous cases become `UNCERTAIN`.

------------------------------------------------------------------------

# Phase 18 --- Risk Analyzer

## Objective

Calculate a reproducible PR risk score.

## Implement

-   deterministic score;
-   severity weighting;
-   impact weighting;
-   test risk;
-   security risk;
-   confidence;
-   explanation.

## Exit Criteria

The same input produces the same score and the score matches documented
test expectations.

------------------------------------------------------------------------

# Phase 19 --- End-to-End GitHub Review

## Objective

Complete the autonomous workflow from real PR to GitHub review.

## Exit Criteria

``` text
Real PR
→ webhook
→ Spring Boot
→ FastAPI
→ PR Analyzer
→ Router
→ LangGraph
→ agents
→ verification
→ risk
→ Spring Boot
→ GitHub comment
```

All stages are observable and traceable.

------------------------------------------------------------------------

# Phase 20 --- React Dashboard

## Objective

Visualize real review data.

## Screens

-   login;
-   repositories;
-   PRs;
-   review status;
-   agent plan;
-   findings;
-   evidence;
-   tests;
-   risk;
-   review history.

## Exit Criteria

Dashboard contains only backend-produced data. No fake/sample review
data in production mode.

------------------------------------------------------------------------

# Phase 21 --- Sandbox and Hardening

## Objective

Secure generated-code execution.

## Implement

-   Docker sandbox;
-   timeout;
-   resource limits;
-   no privileged execution;
-   isolated filesystem;
-   network restrictions where possible.

## Exit Criteria

Generated tests execute in sandbox and cannot access the application
host directly.

------------------------------------------------------------------------

# Phase 22 --- Observability

## Objective

Make every review explainable and debuggable.

Record: - review ID; - commit SHA; - agent execution; - model; - tool; -
duration; - status; - errors; - finding evidence.

## Exit Criteria

A failed review can be diagnosed from logs/traces without guessing.

------------------------------------------------------------------------

# Phase 23 --- Evaluation and Resume Metrics

Measure: - routing accuracy; - false-positive rate; - verified-finding
precision; - test-generation success rate; - average review duration; -
agent execution time; - percentage of PRs requiring LLM ambiguity
routing; - percentage of irrelevant agents skipped.

Do not invent metrics. Measure them from the evaluation suite.

------------------------------------------------------------------------

# Phase 24 --- Deployment

Only after local end-to-end success.

Suggested: - Docker; - GitHub Actions; - cloud deployment; - HTTPS; -
secrets management; - PostgreSQL; - monitoring.

## Final Exit Criterion

A fresh environment can deploy and perform a complete review of a
controlled GitHub PR without manual intervention.
