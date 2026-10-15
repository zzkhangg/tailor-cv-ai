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


class Project(StrictModel):
    name: str
    description: str
    technologies: list[str]
    start_date: str
    end_date: str
    bullets: list[str]


class Certification(StrictModel):
    name: str
    date: str


class Education(StrictModel):
    institution: str
    location: str
    degree: str
    start_date: str
    end_date: str
    details: list[str]


class CanonicalResume(StrictModel):
    name: str
    contact: Contact
    summary: str
    skills: list[str]
    experience: list[Experience]
    projects: list[Project]
    education: list[Education]
    certifications: list[Certification] = Field(default_factory=list)


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
    projects: list[Project]
    education: list[Education]
    certifications: list[Certification]


class TailorRequest(StrictModel):
    resume: CanonicalResume
    job_description: str = Field(min_length=30, max_length=20000)


class TailorModelOutput(StrictModel):
    skill_indices: list[int]
    experience_bullet_indices: list[list[int]]
    missing_requirements: list[str]


class TailorResponse(StrictModel):
    tailored_resume: CanonicalResume
    change_summary: list[str]
    missing_requirements: list[str]
