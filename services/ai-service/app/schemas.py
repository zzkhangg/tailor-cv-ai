from pydantic import BaseModel, ConfigDict, Field


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


class ParseRequest(StrictModel):
    text: str = Field(min_length=1)


class ParseModelOutput(StrictModel):
    name: str
    contact: Contact
    summary: str
    skills: list[str]
    experience: list[Experience]


class TailorRequest(StrictModel):
    resume: CanonicalResume
    job_description: str = Field(min_length=30, max_length=20000)


class TailorModelOutput(StrictModel):
    skills: list[str]
    experience_bullets: list[list[str]]
    missing_requirements: list[str]


class TailorResponse(StrictModel):
    tailored_resume: CanonicalResume
    change_summary: list[str]
    missing_requirements: list[str]
