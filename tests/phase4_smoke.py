"""Exercise the complete Phase 4 workflow with fictional data and one AI call."""

import argparse
import json
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

sys.path.insert(0, str(Path(__file__).parent / "fixtures"))
from make_sample_pdf import make_pdf  # noqa: E402


def request(url: str, *, method: str = "GET", data: bytes | None = None,
            content_type: str | None = None, timeout: int = 20) -> tuple[str, bytes]:
    headers = {"Content-Type": content_type} if content_type else {}
    try:
        with urlopen(Request(url, data=data, headers=headers, method=method), timeout=timeout) as response:
            if response.status not in (200, 201):
                raise RuntimeError(f"{method} {url}: HTTP {response.status}")
            return response.headers.get_content_type(), response.read()
    except HTTPError as error:
        raise RuntimeError(f"{method} {url}: HTTP {error.code}") from error
    except URLError as error:
        raise RuntimeError(f"{method} {url}: connection failed ({error.reason})") from error


def request_json(url: str, **kwargs: object) -> dict:
    _, body = request(url, **kwargs)
    return json.loads(body)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default="http://localhost:8080")
    args = parser.parse_args()
    base = args.api_url.rstrip("/")

    boundary = "tailorcv-phase4-smoke"
    upload = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="fictional-resume.pdf"\r\n'
        "Content-Type: application/pdf\r\n\r\n"
    ).encode() + make_pdf() + f"\r\n--{boundary}--\r\n".encode()
    resume = request_json(
        f"{base}/api/v1/resumes", method="POST", data=upload,
        content_type=f"multipart/form-data; boundary={boundary}",
    )
    job = request_json(
        f"{base}/api/v1/jobs", method="POST",
        data=json.dumps({
            "title": "Fictional Platform Engineer",
            "description": "Build reliable Python and Java services and collaborate with a fictional product team.",
        }).encode(), content_type="application/json",
    )
    tailored = request_json(
        f"{base}/api/v1/tailor", method="POST", timeout=75,
        data=json.dumps({"resume_id": resume["resume_id"], "job_id": job["job_id"]}).encode(),
        content_type="application/json",
    )
    if tailored.get("status") != "SUCCEEDED" or not tailored.get("run_id"):
        raise RuntimeError("Tailoring did not return a successful run")

    run_id = tailored["run_id"]
    preview_type, preview = request(f"{base}/api/v1/tailor/{run_id}/preview")
    if preview_type != "text/html" or b"Jane Doe" not in preview:
        raise RuntimeError("HTML preview was missing expected fictional content")
    pdf_type, pdf = request(f"{base}/api/v1/tailor/{run_id}/pdf")
    if pdf_type != "application/pdf" or not pdf.startswith(b"%PDF") or len(pdf) < 100:
        raise RuntimeError("PDF export was not a valid PDF response")

    print("Phase 4 smoke test passed (upload, job, tailoring, preview, PDF export).")
    print(f"Fictional tailoring run ID: {run_id}")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"Phase 4 smoke test failed: {error}", file=sys.stderr)
        sys.exit(1)
