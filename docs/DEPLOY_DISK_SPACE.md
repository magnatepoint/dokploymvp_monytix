# Deployment: Disk Space Issues

If Dokploy (or Docker) build fails with **"No space left on device"** during `pip install`:

## 1. Free disk on the server (recommended)

SSH into your Dokploy host and run:

```bash
# Check disk usage
df -h
docker system df -v

# Prune Docker (reclaim most space)
docker system prune -a -f
docker builder prune -a -f
```

## 2. Code-level mitigation (already applied)

The backend Dockerfile installs dependencies in **7 batches** instead of one large `pip install`. This reduces peak disk usage during build:

- `requirements-core.txt` – FastAPI, Celery, Redis, etc.
- `requirements-data1.txt` – pandas, openpyxl, xlrd
- `requirements-data2.txt` – pdfplumber, PyMuPDF
- `requirements-ml1.txt` – scipy (~35MB)
- `requirements-ml2.txt` – scikit-learn
- `requirements-google.txt` – Firebase, Google Cloud Pub/Sub

Each batch cleans `/tmp` before the next, which can help on low-disk servers.

## 3. If it still fails

- Increase disk on the VM/instance (e.g. 20GB+ for Docker builds)
- Or build the image elsewhere (CI) and push to a registry, then pull on deploy
