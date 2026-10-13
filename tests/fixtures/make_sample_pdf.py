"""Create a clearly fictional, text-based PDF for Phase 2 API testing."""

from pathlib import Path
import sys

lines = [
    "Jane Doe",
    "jane@example.com",
    "Location: Brisbane",
    "Summary: Software engineering student.",
    "Skills: Python, FastAPI",
    "Experience",
    "Example Co | Software Intern | 2025-01 | 2025-03",
    "- Developed REST APIs using Python.",
]


def make_pdf() -> bytes:
    stream = "BT /F1 12 Tf 50 750 Td 18 TL\n" + "\n".join(
        f"({line}) Tj T*" for line in lines
    ) + "\nET\n"
    stream_bytes = stream.encode("ascii")
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream_bytes)).encode() + b" >>\nstream\n" + stream_bytes + b"endstream",
    ]
    data = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for number, obj in enumerate(objects, 1):
        offsets.append(len(data))
        data.extend(f"{number} 0 obj\n".encode() + obj + b"\nendobj\n")
    startxref = len(data)
    data.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        data.extend(f"{offset:010d} 00000 n \n".encode())
    data.extend(f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{startxref}\n%%EOF\n".encode())
    return bytes(data)


if __name__ == "__main__":
    output = Path(sys.argv[1] if len(sys.argv) > 1 else "sample-resume.pdf")
    output.write_bytes(make_pdf())
    print(f"Created fictional sample: {output}")
