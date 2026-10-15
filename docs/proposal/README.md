# LaTeX proposal

`proposal.tex` is the editable two-page proposal source. Replace `YOUR NAME` and `STUDENT ID`, then update the GKE configuration, cost total, and planned/implemented wording after cloud verification.

Compile twice into a temporary directory so generated files do not enter the repository:

```bash
mkdir -p /tmp/tailorcv-proposal
pdflatex -interaction=nonstopmode -halt-on-error \
  -output-directory=/tmp/tailorcv-proposal docs/proposal/proposal.tex
pdflatex -interaction=nonstopmode -halt-on-error \
  -output-directory=/tmp/tailorcv-proposal docs/proposal/proposal.tex
```

The output is `/tmp/tailorcv-proposal/proposal.pdf`. The top-level `.gitignore` excludes generated PDFs, but temporary output also avoids committing LaTeX `.aux`, `.log`, and `.out` files.
