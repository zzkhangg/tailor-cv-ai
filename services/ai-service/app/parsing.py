"""Conservative Phase 2 parser for clearly labelled text resumes.

AI-based parsing is a later phase. This parser copies only text present in the PDF.
"""

import re

from app.schemas import CanonicalResume, Contact, Experience

EMAIL = re.compile(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", re.IGNORECASE)
EXPERIENCE = re.compile(
    r"^(?P<company>[^|]+)\s*\|\s*(?P<title>[^|]+)\s*\|\s*"
    r"(?P<start>\d{4}-\d{2})\s*\|\s*(?P<end>\d{4}-\d{2}|Present)$",
    re.IGNORECASE,
)
SECTION_NAMES = {
    "summary": "summary", "professional summary": "summary", "career summary": "summary",
    "profile": "summary", "professional profile": "summary", "about me": "summary",
    "personal statement": "summary", "objective": "summary",
    "skills": "skills", "technical skills": "skills", "key skills": "skills",
    "core skills": "skills", "core competencies": "skills", "skills and technologies": "skills",
    "technologies": "skills", "tech stack": "skills",
    "experience": "experience", "work experience": "experience",
    "professional experience": "experience", "employment history": "experience",
    "work history": "experience", "projects": "projects", "education": "education",
    "certifications": "other", "awards": "other", "languages": "other",
    "interests": "other", "references": "other", "volunteer experience": "other",
}
HEADING = re.compile(r"^([A-Za-z][A-Za-z &/-]{0,50}?)(?:\s*[:：-]\s*(.*))?$", re.IGNORECASE)


def section_heading(line: str) -> tuple[str, str] | None:
    match = HEADING.fullmatch(line.strip())
    if not match:
        return None
    name = re.sub(r"\s+", " ", match.group(1)).strip().casefold()
    section = SECTION_NAMES.get(name)
    return (section, (match.group(2) or "").strip()) if section else None


def parse_resume(text: str) -> CanonicalResume:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    name = ""
    if lines and len(lines[0]) <= 100 and "@" not in lines[0] and section_heading(lines[0]) is None:
        name = lines[0]

    email_match = EMAIL.search(text)
    email = email_match.group(0) if email_match else ""
    phone = ""
    location = ""
    summary_lines: list[str] = []
    skills: list[str] = []
    experiences: list[Experience] = []
    section = ""

    for line in lines[1:] if name else lines:
        heading = section_heading(line)
        if heading:
            section, remainder = heading
            if not remainder:
                continue
            line = remainder

        if line.lower().startswith("phone:"):
            phone = line.partition(":")[2].strip()
            continue
        if line.lower().startswith("location:"):
            location = line.partition(":")[2].strip()
            continue
        if section == "summary":
            summary_lines.append(line)
        elif section == "skills":
            skills.extend(part.strip(" \t•-–") for part in re.split(r"[,;|•]", line) if part.strip(" \t•-–"))
        elif section == "experience":
            match = EXPERIENCE.fullmatch(line)
            if match:
                experiences.append(Experience(
                    company=match.group("company").strip(),
                    title=match.group("title").strip(),
                    start_date=match.group("start"),
                    end_date=match.group("end"),
                    bullets=[],
                ))
            elif line.startswith(("- ", "• ")) and experiences:
                experiences[-1].bullets.append(line[2:].strip())

    return CanonicalResume(
        name=name,
        contact=Contact(email=email, phone=phone, location=location),
        summary=" ".join(summary_lines),
        skills=list(dict.fromkeys(skills)),
        experience=experiences,
        projects=[],
        education=[],
    )
