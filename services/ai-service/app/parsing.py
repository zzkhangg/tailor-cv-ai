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
HEADINGS = {"summary", "profile", "skills", "experience", "projects", "education"}


def parse_resume(text: str) -> CanonicalResume:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    name = ""
    if lines and len(lines[0]) <= 100 and "@" not in lines[0] and lines[0].rstrip(":").lower() not in HEADINGS:
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
        label, separator, remainder = line.partition(":")
        if label.lower() in HEADINGS and (separator or line.lower() in HEADINGS):
            section = label.lower()
            if remainder.strip():
                line = remainder.strip()
            else:
                continue
        elif line.lower() in HEADINGS:
            section = line.lower()
            continue

        if line.lower().startswith("phone:"):
            phone = line.partition(":")[2].strip()
            continue
        if line.lower().startswith("location:"):
            location = line.partition(":")[2].strip()
            continue
        if section in {"summary", "profile"}:
            summary_lines.append(line)
        elif section == "skills":
            skills.extend(part.strip() for part in re.split(r"[,;]", line) if part.strip())
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
