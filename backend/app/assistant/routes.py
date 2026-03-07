"""Assistant API routes."""

from fastapi import APIRouter, Depends

from app.auth.dependencies import get_current_user
from app.auth.models import AuthenticatedUser

from .models import AskRequest, AskResponse
from .service import answer_for_prompt

router = APIRouter(prefix="/v1/assistant", tags=["assistant"])


@router.post("/ask", response_model=AskResponse, summary="Ask MONYTIX")
async def ask(
    body: AskRequest,
    user: AuthenticatedUser = Depends(get_current_user),
) -> AskResponse:
    """Submit a prompt and get an answer. Uses rule-based replies; can be extended with LLM."""
    answer = answer_for_prompt(body.prompt)
    return AskResponse(answer=answer)
