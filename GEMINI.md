# Autonomous Code Review Agent --- Gemini Rules

## Mission

Build a real, testable Autonomous Code Review Agent. The system must
analyze GitHub Pull Requests, intelligently route only relevant review
agents, execute those agents, verify findings, calculate risk, and
publish a trustworthy review.

The project is considered successful only when the stated objective is
demonstrated by executable tests and end-to-end evidence.

## Non-Negotiable Engineering Rules

1.  **Do not create fake implementations.**

    -   No placeholder methods that return hard-coded success.
    -   No `TODO` implementation hidden behind a working-looking API.
    -   No mocked LLM/agent output presented as real review results.
    -   No fake GitHub webhook events in production paths.
    -   Mocks/stubs are allowed only inside explicit tests.

2.  **Continuously verify the objective.** After implementing every
    major component, run its tests and verify the acceptance criteria in
    `phases.md`. If the objective is not met, stop feature expansion and
    fix the objective first.

3.  **Evidence over claims.** Every important AI finding must contain
    evidence such as changed code, AST/dependency information,
    static-analysis output, test execution, or repository context.

4.  **Deterministic first, LLM second.** Do not ask an LLM to calculate
    facts that can be obtained reliably from Git, AST parsing, file
    metadata, dependency analysis, test discovery, or static-analysis
    tools. Use an LLM for semantic reasoning and ambiguity.

5.  **Agent Router is not the PR Analyzer.**

    -   PR Analyzer answers: "What changed and what could be affected?"
    -   Agent Router answers: "Which agents should run and why?"
    -   Agents answer: "What problems exist?"
    -   Verification answers: "Is the finding actually valid?"
    -   Risk Analyzer answers: "How risky is this PR?"

6.  **LangGraph is orchestration, not routing intelligence.** The Agent
    Router produces an `AgentExecutionPlan`. LangGraph executes that
    plan, handles state, conditional branches, parallel execution,
    retries, and workflow completion.

7.  **No blind LLM routing.** The Agent Router must use deterministic
    signals and weighted capability scoring first. LLM routing is only
    for ambiguous cases and its output must be schema-validated.

8.  **Every agent has a bounded responsibility.**

    -   Security Agent: security vulnerabilities and security-control
        regressions.
    -   Architecture Agent: architecture/design/layer/dependency
        violations.
    -   Test Agent: test adequacy, test generation, test execution, test
        quality.
    -   Code Quality Agent: maintainability, complexity, duplication,
        style, code smells.

9.  **No duplicate responsibilities disguised as multiple agents.** If
    two agents report the same underlying issue, Finding Merger must
    correlate them before final reporting.

10. **Generated tests must execute.** The Test Agent cannot claim
    success merely because test source code was generated. Generated
    tests must compile and run in an isolated environment.

11. **Generated fixes must be verified before being recommended as
    validated fixes.** If automatic patch generation is implemented, the
    patch must be compiled/tested in an isolated environment.

12. **Never execute untrusted repository or AI-generated code directly
    on the application host.** Use an isolated Docker sandbox for code
    execution.

13. **GitHub webhook security is mandatory.** Verify webhook signatures
    and reject invalid requests.

14. **Review jobs must be idempotent.** A duplicated webhook must not
    create duplicate review state or duplicate final comments.

15. **No silent failure.** Every component must return explicit status,
    error details, and traceable execution metadata.

## Definition of Done

A feature is not "done" because: - the endpoint exists; - the code
compiles; - the UI displays sample data; - an LLM returns plausible
text.

A feature is done only when: - its acceptance tests pass; - the result
is backed by real inputs; - failure cases are tested; - its objective
can be demonstrated from a real or controlled test repository; -
logs/traces show what happened.

## Required Development Discipline

For every component:

1.  Define objective.
2.  Define input/output contract.
3.  Define acceptance tests.
4.  Implement the smallest real version.
5.  Run tests.
6.  Test failure cases.
7.  Inspect actual output.
8.  Fix failures.
9.  Only then move to the next component.

## Project Priorities

Priority order:

1.  PR Analyzer correctness.
2.  Agent Router correctness.
3.  Agent execution correctness.
4.  Verification and risk correctness.
5.  Spring Boot/GitHub integration.
6.  Frontend.
7.  Deployment/observability improvements.

Do not reverse this order merely to make the project look complete.

## Truthfulness

Never report: - "verified" when no verification happened; - "tests
passed" when tests were not executed; - "security issue confirmed" when
the result is only an LLM suspicion; - "agent completed" when the agent
crashed; - "coverage improved" without measured coverage; - "repository
learned" without persisted repository context.

Use statuses such as: `VERIFIED`, `FALSE_POSITIVE`, `UNCERTAIN`,
`FAILED`, `SKIPPED`, `NOT_AVAILABLE`.

## Testing Requirement

Every core component must have: - unit tests; - integration tests where
external boundaries exist; - fixture repositories/PRs; - negative
tests; - deterministic expected outputs where possible.

The project must contain a reproducible evaluation suite for: -
README-only PR; - documentation + architecture description; -
source-code feature PR; - security-sensitive PR; -
architecture-violating PR; - missing-test PR; - test-only PR; -
formatting-only PR; - ambiguous PR requiring LLM routing; - multi-file
high-impact PR.

## Final Objective

The final system must demonstrate:

GitHub PR → Spring Boot webhook → review job → FastAPI → PR Analyzer →
Agent Router → LangGraph → selected agents → finding merger →
verification → risk analysis → final review → Spring Boot persistence →
GitHub review + React dashboard.

Do not move to the next major phase until the current phase's objective
is objectively demonstrated.
