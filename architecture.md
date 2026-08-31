# Autonomous Code Review Agent --- System Architecture

## 1. System Objective

Build an autonomous GitHub Pull Request review platform that:

-   receives PR events through GitHub webhooks;
-   understands the PR using deterministic repository analysis;
-   intelligently selects relevant review agents;
-   executes selected agents through LangGraph;
-   combines deterministic analysis with LLM reasoning;
-   verifies important findings;
-   generates and executes missing tests;
-   calculates PR risk;
-   publishes evidence-backed results to GitHub;
-   visualizes the review in a React dashboard.

## 2. High-Level Architecture

``` text
GitHub
  |
  | Pull Request Webhook
  v
Spring Boot Control Plane
  |
  | REST
  v
FastAPI Agent Service
  |
  v
PR Analyzer
  |
  | PRAnalysisReport
  v
Agent Router
  |
  | AgentExecutionPlan
  v
LangGraph Review Workflow
  |
  +--> Security Agent
  +--> Architecture Agent
  +--> Test Agent
  +--> Code Quality Agent
  |
  v
Finding Merger
  |
  v
Verification Agent
  |
  v
Risk Analyzer
  |
  v
Final Review
  |
  +--> Spring Boot / PostgreSQL
  +--> GitHub PR Review
  +--> React Dashboard
```

## 3. Architectural Boundaries

### Spring Boot --- Control Plane

Responsibilities: - authentication; - authorization; - Google OAuth2 /
application login; - GitHub connection management; - GitHub webhook
reception; - GitHub API integration; - review lifecycle/state; -
persistence; - frontend APIs; - internal communication with FastAPI.

Spring Boot must not contain the core AI/code-analysis agents.

### FastAPI --- Intelligence Plane

Responsibilities: - review orchestration; - PR Analyzer; - Agent
Router; - LangGraph workflow; - four review agents; - LLM abstraction; -
verification; - test generation/execution orchestration; - risk
analysis.

### React --- Presentation Layer

Responsibilities: - authentication UI; - repository/PR selection; -
review status; - agent execution status; - findings; - evidence; -
generated tests; - risk score; - review history.

## 4. PR Analyzer

The PR Analyzer is deterministic-first.

Modules: - `PRMetadataCollector` - `DiffAnalyzer` - `FileClassifier` -
`ASTAnalyzer` - `SymbolAnalyzer` - `DependencyAnalyzer` -
`TestAnalyzer` - `SecuritySignalExtractor` -
`ArchitectureSignalExtractor` - `QualitySignalExtractor` -
`ChangeImpactAnalyzer` - `AnalysisAggregator`

It produces a `PRAnalysisReport`.

The PR Analyzer does not decide which agents run.

## 5. PRAnalysisReport

Minimum conceptual schema:

``` json
{
  "pr": {
    "number": 142,
    "title": "Add payment API",
    "base_sha": "abc",
    "head_sha": "def"
  },
  "summary": {
    "files_changed": 5,
    "lines_added": 142,
    "lines_removed": 31,
    "change_type": "FEATURE"
  },
  "files": [],
  "symbols": [],
  "dependencies": {},
  "tests": {},
  "signals": {
    "security": [],
    "architecture": [],
    "quality": []
  },
  "impact": {}
}
```

Facts must come from Git, AST, repository structure, static analysis,
and test discovery whenever possible.

## 6. Agent Router

The Agent Router consumes `PRAnalysisReport`.

Pipeline:

``` text
PRAnalysisReport
  |
Feature Extraction
  |
Capability Matching
  |
Weighted Rule Scoring
  |
Confidence Evaluation
  |
+--> High-confidence deterministic decision
|
+--> Ambiguous case -> LLM Router
  |
Decision Merger
  |
AgentExecutionPlan
```

It must support: - RUN; - SKIP; - CONDITIONAL; - priority; - relevance
score; - confidence; - reasons; - evidence/signals supporting the
decision.

Example:

``` json
{
  "agent": "SECURITY",
  "decision": "RUN",
  "score": 92,
  "confidence": 0.97,
  "reasons": [
    "JWT authentication code changed"
  ]
}
```

## 7. Routing Examples

### README-only

``` text
Security       -> SKIP
Architecture   -> RUN if architecture documentation changed
Test           -> SKIP
Code Quality   -> CONDITIONAL
```

### Authentication changes

``` text
Security       -> RUN
Architecture   -> RUN
Test           -> RUN
Code Quality   -> RUN
```

### Test-only change

``` text
Security       -> SKIP
Architecture   -> CONDITIONAL
Test           -> RUN
Code Quality   -> RUN
```

### Formatting-only change

``` text
Security       -> SKIP
Architecture   -> SKIP
Test           -> SKIP
Code Quality   -> RUN or SKIP based on formatting policy
```

## 8. LangGraph

LangGraph is the workflow/state orchestration layer.

It receives `AgentExecutionPlan` and executes the selected agents.

Responsibilities: - shared review state; - conditional routing; -
parallel agent execution; - retries; - test generation loops; -
verification branches; - workflow completion; - error propagation.

LangGraph does not replace Agent Router.

Relationship:

``` text
Agent Router:
"Who should run?"

LangGraph:
"Execute the selected plan and manage the workflow."
```

## 9. Review Agents

### Security Agent

Checks: - authentication; - authorization; - JWT/OAuth; - injection; -
XSS; - CSRF; - SSRF; - path traversal; - secrets; - sensitive-data
leakage; - weak cryptography; - insecure deserialization.

Uses: - static security tools; - AST; - data-flow signals; - repository
context; - LLM reasoning.

### Architecture Agent

Checks: - layer violations; - dependency direction; - circular
dependencies; - module boundaries; - separation of concerns; -
repository architecture conventions; - design principles.

Uses: - dependency graph; - AST; - package/module structure; -
architecture documentation; - repository memory; - LLM reasoning.

### Test Agent

Checks: - tests covering changed symbols; - regression coverage; - edge
cases; - test quality.

Can: - discover existing tests; - calculate coverage; - generate missing
tests; - execute tests; - evaluate generated-test quality.

Generated tests must compile and execute.

### Code Quality Agent

Checks: - complexity; - duplication; - maintainability; - naming; - dead
code; - code smells; - repository conventions; - static-analysis
violations.

Uses: - Checkstyle/PMD/SpotBugs for Java; - ESLint for JS/TS; -
Ruff/Pylint where appropriate; - AST metrics; - LLM reasoning.

## 10. Common Agent Interface

All agents should implement a common contract.

Conceptually:

``` python
class ReviewAgent:
    async def analyze(self, context) -> AgentResult:
        ...
```

Each agent returns: - agent name; - status; - findings; - metrics; -
evidence; - execution metadata; - errors if applicable.

## 11. Finding Merger

Responsibilities: - combine findings; - detect duplicate/related
findings; - preserve originating agents; - retain evidence; - normalize
severity/confidence.

It must not silently discard evidence.

## 12. Verification Agent

Every important finding can be challenged.

Verification sources: - source code; - AST; - dependency graph; -
static-analysis result; - repository context; - test/reproduction; -
generated regression test.

Possible outcomes:

``` text
VERIFIED
FALSE_POSITIVE
UNCERTAIN
```

Do not label a finding `VERIFIED` without evidence.

## 13. Risk Analyzer

Inputs: - verified findings; - severity; - confidence; - changed
files; - change impact; - security impact; - architecture impact; - test
impact; - complexity; - coverage.

Output:

``` text
risk_score: 0-100
risk_level: LOW | MEDIUM | HIGH | CRITICAL
factors: [...]
```

The scoring formula must be documented and tested.

## 14. LLM Abstraction

Agents use:

``` text
LLMProvider
  |
  +-- OllamaProvider
  +-- GeminiProvider
  +-- OpenAIProvider
```

Configuration must select the provider without changing agent code.

The LLM must not be responsible for deterministic facts.

## 15. Sandbox

Generated tests/fixes must run inside Docker.

Flow:

``` text
Generated code
  |
  v
Docker sandbox
  |
  +--> compile
  +--> test
  +--> static scan
  |
  v
execution result
```

No untrusted generated/repository code should execute directly in the
main application process.

## 16. Spring Boot Data Model

Core entities:

``` text
User
GitHubConnection
Repository
PullRequest
Review
AgentExecution
Finding
TestExecution
RiskAnalysis
```

Review status:

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

## 17. End-to-End Runtime Flow

``` text
1. Developer opens/updates PR.
2. GitHub sends webhook.
3. Spring Boot verifies webhook.
4. Spring Boot creates/updates PR and Review records.
5. Spring Boot calls FastAPI.
6. FastAPI starts LangGraph review state.
7. PR Analyzer fetches/analyzes the exact base/head revisions.
8. Agent Router creates AgentExecutionPlan.
9. LangGraph executes selected agents.
10. Agents return structured results.
11. Findings are merged.
12. Verification challenges important findings.
13. Test Agent generates/runs tests when required.
14. Risk Analyzer calculates risk.
15. Final Review is created.
16. FastAPI returns result to Spring Boot.
17. Spring Boot persists review.
18. Spring Boot publishes review/comments to GitHub.
19. React dashboard displays the result.
```

## 18. Reliability Rules

-   Use exact commit SHAs for reproducibility.
-   Make review processing idempotent.
-   Record agent versions/model versions.
-   Record prompts or prompt version identifiers where appropriate.
-   Record tool versions.
-   Record execution duration.
-   Record failures.
-   Never overwrite a completed review silently.
-   New PR commits create a new review revision.
