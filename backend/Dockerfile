# Multi-stage build for optimized production image
# Note: build-essential removed to save ~336MB during build (all deps use pre-built wheels)
FROM python:3.11-slim as builder

WORKDIR /app

# Copy dependency files (split to reduce peak disk during build)
COPY pyproject.toml requirements*.txt ./

# Install Python dependencies in small batches to reduce peak disk (helps low-disk servers)
RUN python -m pip install --no-cache-dir --upgrade pip setuptools wheel
RUN pip install --no-cache-dir -r requirements-core.txt && rm -rf /tmp/* /root/.cache/*
RUN pip install --no-cache-dir -r requirements-data1.txt && rm -rf /tmp/* /root/.cache/*
RUN pip install --no-cache-dir -r requirements-data2.txt && rm -rf /tmp/* /root/.cache/*
RUN pip install --no-cache-dir -r requirements-ml1.txt && rm -rf /tmp/* /root/.cache/*
RUN pip install --no-cache-dir -r requirements-ml2.txt && rm -rf /tmp/* /root/.cache/*
RUN pip install --no-cache-dir -r requirements-google.txt && rm -rf /tmp/* /root/.cache/*

# Production stage
FROM python:3.11-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -u 1000 appuser && \
    mkdir -p /app /app/logs /app/data && \
    chown -R appuser:appuser /app

# Set working directory
WORKDIR /app

# Copy Python dependencies from builder
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application code
COPY app/ ./app/
COPY migrations/ ./migrations/
COPY models/ ./models/
COPY deploy/ ./deploy/
COPY gmail-pubsub-key.json* ./

# Set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Use PORT from env (e.g. Dokploy); default 8001
ENV PORT=8001
EXPOSE 8001

# Health check (uses PORT so it works when Dokploy sets PORT)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f "http://localhost:${PORT}/health" || exit 1

# Default command; respect PORT for PaaS (Dokploy, etc.)
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
