from types import SimpleNamespace

from fastapi.testclient import TestClient
import pytest

from app.main import app
from app.schemas import CanonicalResume, TailorModelOutput
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
    output = TailorModelOutput(skill_indices=[1, 0],
                               experience_bullet_indices=[[1, 0]],
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
    ([2], [[0]]),
    ([0], [[2]]),
    ([0, 0], [[0]]),
    ([0], []),
])
def test_grounding_filters_invalid_indices_without_inventing_facts(skills, bullets):
    output = TailorModelOutput(skill_indices=skills, experience_bullet_indices=bullets,
                               missing_requirements=[])
    tailored, _, flagged = verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)
    assert flagged
    assert set(tailored.skills) <= set(SOURCE["skills"])
    assert set(tailored.experience[0].bullets) <= set(SOURCE["experience"][0]["bullets"])


def test_grounding_filters_missing_requirement_not_in_job():
    output = TailorModelOutput(skill_indices=[0, 1], experience_bullet_indices=[[0, 1]],
                               missing_requirements=["Cloud certification"])
    _, missing, flagged = verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)
    assert missing == []
    assert flagged


def test_grounding_filters_requirement_already_in_resume():
    output = TailorModelOutput(skill_indices=[0, 1], experience_bullet_indices=[[0, 1]],
                               missing_requirements=["Python"])
    _, missing, flagged = verify_grounding(CanonicalResume.model_validate(SOURCE), output, JD)
    assert missing == []
    assert flagged


def test_endpoint_flags_unsupported_requirement_but_returns_grounded_resume(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = TailorModelOutput(skill_indices=[1, 0], experience_bullet_indices=[[0, 1]],
                               missing_requirements=["Cloud certification"])
    monkeypatch.setattr("app.tailoring.OpenAI", lambda **kwargs: SimpleNamespace(
        responses=SimpleNamespace(parse=lambda **kwargs: SimpleNamespace(output_parsed=output))))
    response = TestClient(app).post("/internal/tailor", json={"resume": SOURCE, "job_description": JD})
    assert response.status_code == 200
    assert response.json()["tailored_resume"]["skills"] == ["Java", "Python"]
    assert response.json()["missing_requirements"] == []
    assert any("Ignored AI suggestions" in line for line in response.json()["change_summary"])


def test_endpoint_flags_unsupported_ai_indices(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "fictional-test-key")
    output = TailorModelOutput(skill_indices=[2], experience_bullet_indices=[[0, 1]],
                               missing_requirements=[])
    monkeypatch.setattr("app.tailoring.OpenAI", lambda **kwargs: SimpleNamespace(
        responses=SimpleNamespace(parse=lambda **kwargs: SimpleNamespace(output_parsed=output))))
    response = TestClient(app).post("/internal/tailor", json={"resume": SOURCE, "job_description": JD})
    assert response.status_code == 200
    assert response.json()["tailored_resume"]["skills"] == SOURCE["skills"]
    assert any("Ignored AI suggestions" in line for line in response.json()["change_summary"])
