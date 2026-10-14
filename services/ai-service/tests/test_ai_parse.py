from types import SimpleNamespace

from fastapi.testclient import TestClient

from app.main import app
from app.schemas import ParseModelOutput

TEXT = "Jane Doe\njane@example.com\nProfessional Summary\nSoftware engineering student.\nSkills: Python, FastAPI"


def test_rules_remain_default_without_key(monkeypatch):
    monkeypatch.delenv("AI_PARSE_ENABLED", raising=False)
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = TestClient(app).post("/internal/parse", json={"text": TEXT})
    assert response.status_code == 200
    assert response.json()["skills"] == ["Python", "FastAPI"]


def test_ai_parse_requires_key_when_enabled(monkeypatch):
    monkeypatch.setenv("AI_PARSE_ENABLED", "true")
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = TestClient(app).post("/internal/parse", json={"text": TEXT})
    assert response.status_code == 503


def test_ai_parse_accepts_exact_source_facts(monkeypatch):
    monkeypatch.setenv("AI_PARSE_ENABLED", "true")
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = ParseModelOutput(name="Jane Doe", contact={"email": "jane@example.com", "phone": "", "location": ""},
                              summary="Software engineering student.", skills=["Python", "FastAPI"], experience=[])
    monkeypatch.setattr("app.ai_parsing.OpenAI", lambda **kwargs: SimpleNamespace(
        responses=SimpleNamespace(parse=lambda **kwargs: SimpleNamespace(output_parsed=output))))
    response = TestClient(app).post("/internal/parse", json={"text": TEXT})
    assert response.status_code == 200
    assert response.json()["skills"] == ["Python", "FastAPI"]
    assert response.json()["education"] == []


def test_ai_parse_rejects_invented_skill(monkeypatch):
    monkeypatch.setenv("AI_PARSE_ENABLED", "true")
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = ParseModelOutput(name="Jane Doe", contact={"email": "jane@example.com", "phone": "", "location": ""},
                              summary="Software engineering student.", skills=["Kubernetes"], experience=[])
    monkeypatch.setattr("app.ai_parsing.OpenAI", lambda **kwargs: SimpleNamespace(
        responses=SimpleNamespace(parse=lambda **kwargs: SimpleNamespace(output_parsed=output))))
    response = TestClient(app).post("/internal/parse", json={"text": TEXT})
    assert response.status_code == 422
    assert response.json() == {"detail": "AI extraction contained unsupported or invalid facts."}
