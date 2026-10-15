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


def test_parse_multiline_experience_projects_and_certifications():
    text = (
        "Taylor Example\n"
        "Professional Experience\n"
        "Backend Developer Intern Nov. 2025 – Jan. 2026\n"
        "Example Commercial Bank\n"
        "Introductory description that is not a responsibility bullet.\n"
        "Key Responsibilities\n"
        "• Designed REST APIs for accounts, transfers and transaction history, separating request handling\n"
        "and persistence logic.\n"
        "Key Achievements\n"
        "• Wrote unit tests covering transfer validation and ledger entries.\n"
        "Projects\n"
        "SquadGoals — Team Capstone Project May 2026 – Jun. 2026\n"
        "FastAPI, React, TypeScript, Docker\n"
        "• Collaborated in a six-person team on a fictional habit-tracking platform, contributing\n"
        "authentication and notifications.\n"
        "Sample Worker — Distributed Backend System Mar. 2026 – Apr. 2026\n"
        "FastAPI, Celery, PostgreSQL\n"
        "• Built an asynchronous image-analysis backend.\n"
        "Education\n"
        "Example University | Brisbane, QLD\n"
        "Bachelor of Engineering (Honours) Feb. 2025 – Dec. 2026 (Expected)\n"
        "CGPA: 6.5/7.0\n"
        "Sample Institute\n"
        "Bachelor of Computer Science Oct. 2022 – Feb. 2025\n"
        "Academic Scholarship\n"
        "Certifications & Awards\n"
        "Example English Certificate — July 2026\n"
        "Silver Medal in Physics — April 2021\n"
    )
    data = TestClient(app).post("/internal/parse", json={"text": text}).json()
    assert data["experience"] == [{
        "company": "Example Commercial Bank", "title": "Backend Developer Intern",
        "start_date": "Nov. 2025", "end_date": "Jan. 2026",
        "bullets": [
            "Designed REST APIs for accounts, transfers and transaction history, separating request handling and persistence logic.",
            "Wrote unit tests covering transfer validation and ledger entries.",
        ],
    }]
    assert data["projects"][0]["name"] == "SquadGoals"
    assert data["projects"][0]["description"] == "Team Capstone Project"
    assert data["projects"][0]["technologies"] == ["FastAPI", "React", "TypeScript", "Docker"]
    assert data["projects"][0]["bullets"][0].endswith("authentication and notifications.")
    assert data["projects"][1]["name"] == "Sample Worker"
    assert data["education"] == [
        {
            "institution": "Example University", "location": "Brisbane, QLD",
            "degree": "Bachelor of Engineering (Honours)",
            "start_date": "Feb. 2025", "end_date": "Dec. 2026 (Expected)", "details": ["CGPA: 6.5/7.0"],
        },
        {
            "institution": "Sample Institute", "location": "",
            "degree": "Bachelor of Computer Science",
            "start_date": "Oct. 2022", "end_date": "Feb. 2025", "details": ["Academic Scholarship"],
        },
    ]
    assert data["certifications"] == [
        {"name": "Example English Certificate", "date": "July 2026"},
        {"name": "Silver Medal in Physics", "date": "April 2021"},
    ]
