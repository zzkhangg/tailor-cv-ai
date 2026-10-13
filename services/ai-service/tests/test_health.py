import json
from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app
from app.schemas import CanonicalResume


def test_health():
    response = TestClient(app).get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_contract_fixture():
    fixture = Path(__file__).resolve().parents[3] / "docs/contracts/canonical-resume.json"
    data = json.loads(fixture.read_text(encoding="utf-8"))
    assert CanonicalResume.model_validate(data).model_dump() == data
