from pydantic import BaseModel, ConfigDict


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


class Contact(StrictModel):
    email: str
    phone: str
    location: str


class Experience(StrictModel):
    company: str
    title: str
    start_date: str
    end_date: str
    bullets: list[str]


class CanonicalResume(StrictModel):
    name: str
    contact: Contact
    summary: str
    skills: list[str]
    experience: list[Experience]
    projects: list[dict]
    education: list[dict]


class HealthResponse(StrictModel):
    status: str
