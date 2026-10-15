"""Conservative Phase 2 parser for clearly labelled text resumes.

AI-based parsing is a later phase. This parser copies only text present in the PDF.
"""

import re

from app.schemas import CanonicalResume, Certification, Contact, Experience, Project

EMAIL = re.compile(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", re.IGNORECASE)
EXPERIENCE = re.compile(
    r"^(?P<company>[^|]+)\s*\|\s*(?P<title>[^|]+)\s*\|\s*"
    r"(?P<start>\d{4}-\d{2})\s*\|\s*(?P<end>\d{4}-\d{2}|Present)$",
    re.IGNORECASE,
)
MONTH = r"(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\.?"
DATE = rf"(?:{MONTH}\s+\d{{4}}|\d{{4}}-\d{{2}})"
DATED_ENTRY = re.compile(
    rf"^(?P<label>.+?)\s+(?P<start>{DATE})\s*[–—-]\s*(?P<end>{DATE}|Present)$",
    re.IGNORECASE,
)
DATED_CERTIFICATION = re.compile(
    rf"^(?P<name>.+?)\s+[–—-]\s+(?P<date>{DATE})$", re.IGNORECASE,
)
SECTION_NAMES = {
    "summary": "summary", "professional summary": "summary", "career summary": "summary",
    "profile": "summary", "professional profile": "summary", "about me": "summary",
    "personal statement": "summary", "objective": "summary",
    "skills": "skills", "technical skills": "skills", "key skills": "skills",
    "core skills": "skills", "core competencies": "skills", "skills and technologies": "skills",
    "technologies": "skills", "tech stack": "skills", "competencies": "skills",
    "experience": "experience", "work experience": "experience",
    "professional experience": "experience", "employment history": "experience",
    "work history": "experience", "projects": "projects", "project experience": "projects",
    "education": "education", "certifications": "certifications",
    "certifications & awards": "certifications", "certifications and awards": "certifications",
    "awards": "other", "languages": "other",
    "interests": "other", "references": "other", "volunteer experience": "other",
}
HEADING = re.compile(r"^([A-Za-z][A-Za-z &/-]{0,50}?)(?:\s*[:：-]\s*(.*))?$", re.IGNORECASE)
SKILL_CATEGORIES = {
    "languages", "programming languages", "backend", "frontend", "databases",
    "cloud & devops", "cloud and devops", "testing", "tools",
    "professional competencies", "frameworks", "libraries",
}
CATEGORY = re.compile(r"^([^:：]{2,40})[:：]\s*(.+)$")


def split_skills(line: str) -> list[str]:
    return [part.strip(" \t•-–") for part in re.split(r"[,;|•]", line) if part.strip(" \t•-–")]


def bullet_text(line: str) -> str | None:
    match = re.match(r"^(?:[-•▪◦‣]|\d+[.)])\s*(.+)$", line)
    return match.group(1).strip() if match else None


def append_wrapped(items: list[str], line: str) -> None:
    if items:
        items[-1] = f"{items[-1]} {line}".strip()


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
    projects: list[Project] = []
    certifications: list[Certification] = []
    section = ""
    experience_needs_company = False
    project_needs_technologies = False

    for line in lines[1:] if name else lines:
        # Category labels inside Competencies are not top-level sections.
        category = CATEGORY.fullmatch(line) if section == "skills" else None
        if category and category.group(1).strip().casefold() in SKILL_CATEGORIES:
            skills.extend(split_skills(category.group(2)))
            continue

        heading = section_heading(line)
        if heading:
            section, remainder = heading
            experience_needs_company = False
            project_needs_technologies = False
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
            if skills and re.match(r"^(and|or)\b", line, re.IGNORECASE):
                skills[-1] += " " + line
            else:
                skills.extend(split_skills(line))
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
                experience_needs_company = False
                continue
            dated = DATED_ENTRY.fullmatch(line)
            if dated:
                experiences.append(Experience(
                    company="", title=dated.group("label").strip(),
                    start_date=dated.group("start"), end_date=dated.group("end"), bullets=[],
                ))
                experience_needs_company = True
                continue
            bullet = bullet_text(line)
            if bullet and experiences:
                experiences[-1].bullets.append(bullet)
            elif experience_needs_company and experiences:
                experiences[-1].company = line
                experience_needs_company = False
            elif experiences and experiences[-1].bullets and line.casefold() not in {
                "key responsibilities", "key achievements", "responsibilities", "achievements",
            }:
                append_wrapped(experiences[-1].bullets, line)
        elif section == "projects":
            dated = DATED_ENTRY.fullmatch(line)
            if dated:
                label = dated.group("label").strip()
                parts = re.split(r"\s+[—–]\s+", label, maxsplit=1)
                projects.append(Project(
                    name=parts[0].strip(), description=parts[1].strip() if len(parts) > 1 else "",
                    technologies=[], start_date=dated.group("start"), end_date=dated.group("end"), bullets=[],
                ))
                project_needs_technologies = True
                continue
            bullet = bullet_text(line)
            if bullet and projects:
                projects[-1].bullets.append(bullet)
                project_needs_technologies = False
            elif project_needs_technologies and projects:
                projects[-1].technologies = split_skills(line)
                project_needs_technologies = False
            elif projects and projects[-1].bullets:
                append_wrapped(projects[-1].bullets, line)
        elif section == "certifications":
            dated = DATED_CERTIFICATION.fullmatch(line)
            if dated:
                certifications.append(Certification(name=dated.group("name").strip(), date=dated.group("date")))
            else:
                certifications.append(Certification(name=line, date=""))

    return CanonicalResume(
        name=name,
        contact=Contact(email=email, phone=phone, location=location),
        summary=" ".join(summary_lines),
        skills=list(dict.fromkeys(skills)),
        experience=experiences,
        projects=projects,
        education=[],
        certifications=certifications,
    )
