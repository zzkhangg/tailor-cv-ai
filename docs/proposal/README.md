# LaTeX proposal

`proposal.tex` is the editable two-page proposal source. It reflects the configured GKE topology and the cloud run reported by the project operator. The GKE cluster/node/boot-disk subtotal comes from a Google Cloud Pricing Calculator CSV exported on 20 October 2026; other resources remain estimates, not a billing statement.

Compile twice into a temporary directory so generated files do not enter the repository:

```bash
mkdir -p /tmp/tailorcv-proposal
pdflatex -interaction=nonstopmode -halt-on-error \
  -output-directory=/tmp/tailorcv-proposal docs/proposal/proposal.tex
pdflatex -interaction=nonstopmode -halt-on-error \
  -output-directory=/tmp/tailorcv-proposal docs/proposal/proposal.tex
```

The output is `/tmp/tailorcv-proposal/proposal.pdf`. The top-level `.gitignore` excludes generated PDFs, but temporary output also avoids committing LaTeX `.aux`, `.log`, and `.out` files.
