"""Exercise the Phase 2 public API using fictional data only."""

import argparse
import json
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

sys.path.insert(0, str(Path(__file__).parent / "fixtures"))
from make_sample_pdf import make_pdf  # noqa: E402


def request_json(url: str, *, method: str = "GET", data: bytes | None = None,
                 content_type: str | None = None) -> dict:
    headers = {"Content-Type": content_type} if content_type else {}
    request = Request(url, data=data, headers=headers, method=method)
    try:
        with urlopen(request, timeout=15) as response:
            if response.status not in (200, 201):
                raise RuntimeError(f"{method} {url}: HTTP {response.status}")
            return json.load(response)
    except HTTPError as error:
        raise RuntimeError(f"{method} {url}: HTTP {error.code}") from error
    except URLError as error:
        raise RuntimeError(f"{method} {url}: connection failed ({error.reason})") from error


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default="http://localhost:8080")
    args = parser.parse_args()
    base = args.api_url.rstrip("/")

    health = request_json(f"{base}/api/v1/health")
    if health.get("status") != "UP":
        raise RuntimeError("Java health check did not return UP")

    boundary = "tailorcv-phase2-smoke"
    pdf = make_pdf()
    body = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="fictional-resume.pdf"\r\n'
        "Content-Type: application/pdf\r\n\r\n"
    ).encode() + pdf + f"\r\n--{boundary}--\r\n".encode()
    resume = request_json(
        f"{base}/api/v1/resumes", method="POST", data=body,
        content_type=f"multipart/form-data; boundary={boundary}",
    )
    resume_id = resume.get("resume_id")
    if not resume_id or resume.get("parsed_resume", {}).get("name") != "Jane Doe":
        raise RuntimeError("Resume upload did not return the expected parsed sample")

    saved = request_json(f"{base}/api/v1/resumes/{resume_id}")
    if saved.get("resume_id") != resume_id:
        raise RuntimeError("Saved resume could not be retrieved")

    job_body = json.dumps({
        "title": "Sample Software Engineer",
        "description": "Build reliable Java and Python services for a fictional team.",
    }).encode()
    job = request_json(
        f"{base}/api/v1/jobs", method="POST", data=job_body,
        content_type="application/json",
    )
    job_id = job.get("job_id")
    if not job_id:
        raise RuntimeError("Job creation did not return a job ID")

    print("Phase 2 smoke test passed (health, PDF upload, resume retrieval, job creation).")
    print(f"Sample resume ID: {resume_id}")
    print(f"Sample job ID: {job_id}")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, KeyError) as error:
        print(f"Phase 2 smoke test failed: {error}", file=sys.stderr)
        sys.exit(1)
