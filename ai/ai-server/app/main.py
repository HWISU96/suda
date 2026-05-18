from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, status
from fastapi.responses import JSONResponse

from app.model_service import ModelNotLoadedError, SignModelService
from app.schemas import (
    SignInferenceRequest,
    SignInferenceResponse,
)

sign_model_service = SignModelService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    sign_model_service.load()
    yield


app = FastAPI(title="Sign AI Server", version="0.2.0", lifespan=lifespan)


@app.get("/health")
def health() -> JSONResponse:
    health_status = sign_model_service.health()
    http_status = status.HTTP_200_OK if sign_model_service.is_loaded else status.HTTP_503_SERVICE_UNAVAILABLE
    return JSONResponse(status_code=http_status, content=health_status)


@app.post("/internal/sign/predict", response_model=SignInferenceResponse)
def predict(request: SignInferenceRequest) -> SignInferenceResponse:
    try:
        return sign_model_service.predict(request)
    except ModelNotLoadedError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        ) from exc
