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


def test_parse_common_headings_and_stop_at_unrelated_section():
    text = (
        "Alex Example\n"
        "PROFESSIONAL SUMMARY\n"
        "Backend developer with three years of experience.\n"
        "Technical Skills: Python | FastAPI | PostgreSQL\n"
        "- Docker\n"
        "Certifications\n"
        "Example Certificate\n"
    )
    response = TestClient(app).post("/internal/parse", json={"text": text})
    assert response.status_code == 200
    data = response.json()
    assert data["summary"] == "Backend developer with three years of experience."
    assert data["skills"] == ["Python", "FastAPI", "PostgreSQL", "Docker"]


def test_parse_profile_with_inline_text_and_key_skills():
    text = "Sam Sample\nProfessional Profile: Data analyst.\nKey Skills\nSQL\nExcel\nWork History\nExample Co"
    data = TestClient(app).post("/internal/parse", json={"text": text}).json()
    assert data["summary"] == "Data analyst."
    assert data["skills"] == ["SQL", "Excel"]


def test_parse_competencies_with_categories_and_wrapped_text():
    # Fictional CV with the same layout as a real-world category-based resume.
    text = (
        "Taylor Example\n"
        "Professional Summary\n"
        "Backend engineer experienced with reliable services.\n"
        "Competencies\n"
        "Languages: Java, Python, SQL\n"
        "Backend: Spring Boot, FastAPI, REST APIs\n"
        "Databases: PostgreSQL, Redis\n"
        "Cloud & DevOps: Docker, AWS\n"
        "Professional Competencies: API design, database\n"
        "and transactional reasoning\n"
        "Professional Experience\n"
        "Example Company\n"
        "Certifications & Awards\n"
        "Example Award\n"
    )
    data = TestClient(app).post("/internal/parse", json={"text": text}).json()
    assert data["name"] == "Taylor Example"
    assert data["summary"] == "Backend engineer experienced with reliable services."
    assert data["skills"] == [
        "Java", "Python", "SQL", "Spring Boot", "FastAPI", "REST APIs",
        "PostgreSQL", "Redis", "Docker", "AWS", "API design",
        "database and transactional reasoning",
    ]
