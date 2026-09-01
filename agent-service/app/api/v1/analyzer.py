from fastapi import APIRouter, HTTPException, status
from app.schemas.pr_analysis import PRAnalysisRequest, PRAnalysisReport
from app.analyzer.pr_analyzer import PRAnalyzer

router = APIRouter(prefix="/analyze", tags=["Deterministic PR Analyzer"])


@router.post(
    "/pr",
    response_model=PRAnalysisReport,
    status_code=status.HTTP_200_OK,
    summary="Analyze Pull Request Diff deterministically",
    description="Parses Unified Git Diff, extracts AST symbols, maps tests, and detects capability signals without calling an LLM.",
)
async def analyze_pull_request(request: PRAnalysisRequest) -> PRAnalysisReport:
    try:
        return PRAnalyzer.analyze(request)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"PR analysis failed: {str(e)}",
        )
