from fastapi import FastAPI

from app.parsing import parse_resume
from app.schemas import CanonicalResume, HealthResponse, ParseRequest

app = FastAPI(title="TailorCV AI Service", version="0.1.0")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="UP")


@app.post("/internal/parse", response_model=CanonicalResume)
def parse(request: ParseRequest) -> CanonicalResume:
    return parse_resume(request.text)
