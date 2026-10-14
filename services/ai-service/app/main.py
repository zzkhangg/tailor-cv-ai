from fastapi import FastAPI

from app.ai_parsing import parse_with_ai_or_rules
from app.schemas import CanonicalResume, HealthResponse, ParseRequest, TailorRequest, TailorResponse
from app.tailoring import tailor

app = FastAPI(title="TailorCV AI Service", version="0.1.0")


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="UP")


@app.post("/internal/parse", response_model=CanonicalResume)
def parse(request: ParseRequest) -> CanonicalResume:
    return parse_with_ai_or_rules(request.text)


@app.post("/internal/tailor", response_model=TailorResponse)
def tailor_resume(request: TailorRequest) -> TailorResponse:
    return tailor(request)
