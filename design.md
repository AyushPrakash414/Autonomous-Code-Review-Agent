# Autonomous Code Review Agent --- Detailed Design

## 1. Design Philosophy

The system must be evidence-driven and deterministic wherever possible.

The design follows:

``` text
Facts -> Analysis -> Routing -> Specialized Review -> Verification -> Risk
```

LLMs provide semantic reasoning, not basic repository facts.

## 2. Core Domain Objects

### ReviewContext

Contains: - PR metadata; - base/head SHA; - diff; - changed files; -
changed symbols; - dependency graph; - test mapping; - repository
architecture; - extracted signals; - impact analysis.

### AgentExecutionPlan

Contains one decision per agent:

``` json
{
  "agent": "TEST",
  "decision": "RUN",
  "priority": "HIGH",
  "score": 89,
  "confidence": 0.95,
  "reasons": [
    "business logic changed",
    "public method modified"
  ]
}
```

### AgentResult

Contains: - agent; - status; - findings; - metrics; - evidence; -
generated artifacts; - execution duration; - error information.

### Finding

Contains: - title; - description; - file; - line/range; - severity; -
confidence; - evidence; - recommendation; - originating agents; -
verification status.

## 3. PR Analyzer Detailed Design

### Metadata collector

Input: - repository; - PR number; - base SHA; - head SHA.

Output: - PR metadata.

### Diff analyzer

Extract: - added lines; - removed lines; - modified files; - binary
files; - file renames; - change type.

### File classifier

Classify: - source; - test; - documentation; - configuration; - build; -
infrastructure; - CI/CD; - generated file.

### AST analyzer

Extract: - classes; - methods; - functions; - annotations; - imports; -
calls; - control-flow constructs; - changed symbols.

### Dependency analyzer

Build: - direct dependencies; - dependents; - module relationships; -
possible dependency cycles; - changed architectural boundaries.

### Test analyzer

Find: - test files; - related tests; - test framework; - coverage if
available; - changed-symbol-to-test relationships.

### Signal extraction

Signals are facts, not final findings.

Examples: - dynamic SQL; - authentication annotation; - direct
controller-to-repository dependency; - high complexity; - missing test
mapping.

### Impact analyzer

Calculate: - behavioral impact; - module impact; - test impact; -
security sensitivity; - architecture sensitivity.

## 4. Agent Router Detailed Design

Use a capability registry.

Example:

``` json
{
  "SECURITY": {
    "capabilities": [
      "authentication",
      "authorization",
      "input_validation",
      "secrets",
      "injection",
      "sensitive_data"
    ]
  },
  "ARCHITECTURE": {
    "capabilities": [
      "layering",
      "dependency_direction",
      "module_boundaries",
      "design"
    ]
  }
}
```

Scoring must be explainable.

A generic score:

``` text
agent_score =
    capability_match
  + semantic_impact
  + file/layer relevance
  + security/behavior signals
  + dependency impact
  + test impact
  - irrelevant_change_penalty
```

Use configurable weights rather than hard-coded logic scattered through
the codebase.

### Thresholds

Suggested starting policy:

``` text
70-100 -> RUN
40-69  -> CONDITIONAL / ambiguity check
0-39   -> SKIP
```

These thresholds must be configurable and evaluated against the routing
test suite.

### LLM ambiguity routing

Only invoke the LLM when: - score is in the ambiguity range; -
conflicting deterministic signals exist; - content semantics cannot be
classified reliably.

LLM output must: - use a strict schema; - include reasoning; - include
confidence; - reference available signals; - never invent repository
facts.

## 5. LangGraph Design

Suggested graph:

``` text
START
  |
  v
LOAD_REVIEW_CONTEXT
  |
  v
PR_ANALYSIS
  |
  v
AGENT_ROUTING
  |
  +----> SECURITY_AGENT --------+
  |                             |
  +----> ARCHITECTURE_AGENT ----+
  |                             |
  +----> TEST_AGENT ------------+--> FINDING_MERGER
  |                             |
  +----> CODE_QUALITY_AGENT ----+
                                |
                                v
                         VERIFICATION
                                |
                    +-----------+-----------+
                    |                       |
                 VERIFIED                INVALID
                    |                       |
                    v                       v
               RISK_ANALYSIS             DISCARD
                    |
                    v
               FINAL_REVIEW
                    |
                    v
                   END
```

The graph must dynamically skip agents based on `AgentExecutionPlan`.

## 6. Test Agent Subgraph

``` text
TEST_AGENT
   |
   v
DISCOVER_TESTS
   |
   v
TESTS_SUFFICIENT?
   |              |
  YES             NO
   |              |
RUN_TESTS     GENERATE_TESTS
                  |
             VALIDATE_TESTS
                  |
               RUN_TESTS
                  |
             TEST_RESULT
```

Maximum retry count must be configured.

## 7. Verification Design

For each high-impact finding:

``` text
Finding
  |
Evidence collection
  |
Static check
  |
Repository context
  |
Optional reproduction
  |
Verification decision
```

Verification confidence must be separate from detector confidence.

## 8. Risk Design

Risk should not be an arbitrary LLM number.

Use a deterministic scoring model first.

Example factors:

``` text
security severity
architecture impact
behavioral impact
test weakness
change size
dependency impact
finding confidence
```

The LLM can explain the risk but should not be the sole source of the
score.

## 9. Spring Boot Design

### Controllers

``` text
AuthController
GitHubWebhookController
RepositoryController
PullRequestController
ReviewController
FindingController
DashboardController
```

### Services

``` text
AuthService
GitHubService
WebhookService
ReviewService
AgentServiceClient
ReviewResultService
GitHubReviewPublisher
```

### Persistence

Use PostgreSQL with JPA.

### Internal communication

Spring Boot calls FastAPI over authenticated internal REST.

No Redis is required in the initial architecture.

## 10. GitHub Webhook Design

Events: - `pull_request.opened` - `pull_request.synchronize` -
`pull_request.reopened` - optionally `pull_request.closed`

For each event: 1. verify signature; 2. identify repository; 3. identify
PR; 4. identify head SHA; 5. create/update review revision; 6. trigger
FastAPI.

## 11. Frontend Design

Screens:

1.  Login
2.  GitHub connection
3.  Repository list
4.  Pull Request list
5.  PR review dashboard
6.  Agent execution details
7.  Finding details
8.  Test results
9.  Review history

The PR dashboard should expose: - risk score; - review status; -
selected/skipped agents; - agent reasons; - findings; - confidence; -
verification state; - generated tests; - test results; - GitHub comment
status.

## 12. Observability

Every review should have: - review ID; - PR number; - commit SHA; -
agent execution IDs; - start/end times; - status; - error details; -
model/provider; - tool versions.

Use structured logs.

## 13. Failure Handling

Examples:

### GitHub unavailable

Review: `FAILED` Reason: `GITHUB_FETCH_FAILED`

### AST parser failure

Continue with limited analysis if safe: `PARTIAL_ANALYSIS`

### LLM unavailable

Use deterministic-only capabilities when possible. Do not fabricate
results.

### Agent timeout

Mark: `TIMEOUT`

Continue with other independent agents if possible.

### Test generation fails

Mark test generation as: `FAILED` Do not claim tests passed.

## 14. Security Design

-   webhook signature verification;
-   encrypted GitHub credentials/tokens;
-   JWT validation;
-   internal service authentication;
-   least-privilege GitHub access;
-   sandboxed execution;
-   no secrets in logs;
-   input validation;
-   repository access authorization.

## 15. Non-Goals for Initial Version

Do not initially implement: - dozens of programming languages; -
automatic PR merging; - unrestricted autonomous code modification; -
distributed Kubernetes deployment; - complex event streaming; - every
possible security rule.

Start with 2-3 languages and make the core pipeline reliable.
