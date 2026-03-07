"""Assistant (Ask MONYTIX) service – rule-based answers; can plug LLM later."""


def answer_for_prompt(prompt: str) -> str:
    """Return a rule-based answer for the given prompt (mirrors client mock)."""
    p = (prompt or "").strip().lower()
    if "afford" in p or "big purchase" in p:
        return (
            "Based on your current cash flow and goals, you're on track. "
            "Check the Future tab for a 14-day projection. "
            "For big purchases, we recommend keeping 3 months of expenses as buffer."
        )
    if "run short" in p or "dip" in p or "short" in p:
        return (
            "Your forecast shows a dip around days 8–10. "
            "Consider delaying non-essential spend until after payday, or top up your Emergency goal. "
            "See the Financial Future tab for details."
        )
    if "goal" in p or "target" in p:
        return (
            "Use the Goals tab to set targets and track progress. "
            "We'll show projected completion and suggest monthly amounts. "
            "Turn intentions into goals and we'll keep you on track."
        )
    if "spend" in p or "spending" in p or "save" in p:
        return (
            "Check SpendSense for categories and trends. "
            "The Home command center shows top insights. "
            "Use the Future tab for cash flow and the Goals tab to direct surplus."
        )
    # Default
    return (
        "Your finances look on track. "
        "Use the Future tab for projections and Goals for targets. "
        "If you have a specific question, try one of the prompts above."
    )
