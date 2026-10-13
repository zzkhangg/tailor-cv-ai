from fastapi.testclient import TestClient

from app.main import app


def test_parse_copies_labelled_facts():
    text = (
        "Jane Doe\n"
        "jane@example.com\n"
        "Location: Brisbane\n"
        "Summary: Software engineering student.\n"
        "Skills: Python, FastAPI\n"
        "Experience\n"
        "Example Co | Software Intern | 2025-01 | 2025-03\n"
        "- Developed REST APIs using Python.\n"
    )
    response = TestClient(app).post("/internal/parse", json={"text": text})
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "Jane Doe"
    assert data["contact"]["email"] == "jane@example.com"
    assert data["skills"] == ["Python", "FastAPI"]
    assert data["experience"][0]["company"] == "Example Co"
    assert data["experience"][0]["bullets"] == ["Developed REST APIs using Python."]
    assert data["education"] == []


def test_parse_rejects_empty_text():
    response = TestClient(app).post("/internal/parse", json={"text": ""})
    assert response.status_code == 422
