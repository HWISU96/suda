# Sign AI Server

FastAPI server for server-side sign inference with the V6 PyTorch TCN model.

The `.pt` model file is not committed to the repository. Provide it with
`SIGN_MODEL_PATH` when running locally, or mount it to `/models` when running with Docker.

## Run

```bash
$env:SIGN_MODEL_PATH="C:\path\to\best_sign_model_v6.pt"
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## Docker

```bash
docker compose up --build ai-server
```

The default Compose service exposes port `8000` only inside the Docker network. If you need to call
the AI server directly from the host, run it locally with `uvicorn` or open a localhost-only port in
a development override compose file.

For Docker, place `best_sign_model_v6.pt` in the host directory configured by
`SIGN_MODEL_HOST_PATH` so it is mounted as `/models/best_sign_model_v6.pt`.

When Spring runs in Docker Compose, it should call this service with
`SIGN_AI_BASE_URL=http://ai-server:8000`.

When Spring runs locally, it should call this service with
`SIGN_AI_BASE_URL=http://localhost:8000`.

## Endpoints

- `GET /health`
- `POST /internal/sign/predict`

The prediction endpoint expects a flattened `30 * 332` feature sequence and returns top candidates,
confidence, margin, and acceptance metadata.
