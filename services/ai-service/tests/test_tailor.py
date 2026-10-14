from types import SimpleNamespace

from fastapi.testclient import TestClient
import pytest

from app.main import app
from app.schemas import CanonicalResume, TailorModelOutput, TailorRequest
from app.tailoring import verify_grounding


SOURCE = {
    "name": "Jane Doe",
    "contact": {"email": "jane@example.com", "phone": "", "location": ""},
    "summary": "Software engineer.",
    "skills": ["Python", "Java"],
    "experience": [{"company": "Example Co", "title": "Intern", "start_date": "2025-01",
                    "end_date": "2025-03", "bullets": ["Built APIs.", "Wrote tests."]}],
    "projects": [], "education": [],
}
JD = "Build Python services and deploy them with Kubernetes."


def test_tailor_requires_key(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = TestClient(app).post("/internal/tailor", json={"resume": SOURCE, "job_description": JD})
    assert response.status_code == 503
    assert response.json() == {"detail": "AI tailoring is not configured."}


def test_tailor_reorders_only_source_facts(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = TailorModelOutput(skills=["Java", "Python"],
                               experience_bullets=[["Wrote tests.", "Built APIs."]],
                               missing_requirements=["Kubernetes"])
    calls = []

    class FakeResponses:
        def parse(self, **kwargs):
            calls.append(kwargs)
            return SimpleNamespace(output_parsed=output)

    monkeypatch.setattr("app.tailoring.OpenAI", lambda **kwargs: SimpleNamespace(responses=FakeResponses()))
    response = TestClient(app).post("/internal/tailor", json={"resume": SOURCE, "job_description": JD})
    assert response.status_code == 200
    assert response.json()["tailored_resume"]["skills"] == ["Java", "Python"]
    assert response.json()["missing_requirements"] == ["Kubernetes"]
    assert len(response.json()["change_summary"]) == 2
    assert calls[0]["store"] is False
    assert calls[0]["text_format"] is TailorModelOutput


@pytest.mark.parametrize("skills,bullets", [
    (["Kubernetes"], [["Built APIs."]]),
    (["Python"], [["Led a team of 100."]]),
    (["Python"], []),
])
def test_grounding_rejects_unsupported_resume_facts(skills, bullets):
    output = TailorModelOutput(skills=skills, experience_bullets=bullets,
                               missing_requirements=[])
    with pytest.raises(ValueError):
        verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)


def test_grounding_rejects_missing_requirement_not_in_job():
    output = TailorModelOutput(skills=SOURCE["skills"], experience_bullets=[SOURCE["experience"][0]["bullets"]],
                               missing_requirements=["Cloud certification"])
    with pytest.raises(ValueError):
        verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)


def test_grounding_rejects_requirement_already_in_resume():
    output = TailorModelOutput(skills=SOURCE["skills"], experience_bullets=[SOURCE["experience"][0]["bullets"]],
                               missing_requirements=["Python"])
    with pytest.raises(ValueError):
        verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)


def test_endpoint_rejects_unsupported_ai_output(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = TailorModelOutput(skills=["Kubernetes"], experience_bullets=[SOURCE["experience"][0]["bullets"]],
                               missing_requirements=[])
    monkeypatch.setattr("app.tailoring.OpenAI", lambda **kwargs: SimpleNamespace(
        responses=SimpleNamespace(parse=lambda **kwargs: SimpleNamespace(output_parsed=output))))
    response = TestClient(app).post("/internal/tailor", json={"resume": SOURCE, "job_description": JD})
    assert response.status_code == 422
    assert response.json() == {"detail": "AI result contained unsupported or invalid claims."}
