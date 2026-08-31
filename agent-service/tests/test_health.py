import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_root_health_endpoint():
    """Test GET /health returns 200 with valid UP status."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "service" in data
    assert "version" in data
    assert "timestamp" in data


def test_api_v1_health_endpoint():
    """Test GET /api/v1/health returns 200 with structured details."""
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "Autonomous Code Review - Agent Service"
    assert "version" in data
    assert "timestamp" in data
    assert "details" in data
    assert data["details"]["intelligence_plane"] == "active"
    assert data["details"]["llm_provider"] is not None


def test_health_endpoint_method_not_allowed():
    """Negative test: POST /health should return 405 Method Not Allowed."""
    response = client.post("/health")
    assert response.status_code == 405


def test_unknown_endpoint_returns_404():
    """Negative test: GET non-existent route should return 404 Not Found."""
    response = client.get("/api/v1/unknown-endpoint")
    assert response.status_code == 404
