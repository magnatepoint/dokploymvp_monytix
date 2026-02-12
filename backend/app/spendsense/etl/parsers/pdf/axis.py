"""Axis Bank PDF statement line-based parser."""

from __future__ import annotations


def parse_axis_pdf(lines: list[str]) -> None:  # Returns None - placeholder for future implementation
    """Parse Axis Bank PDF statements. Placeholder - enhance when sample PDF available."""
    if not lines:
        return None

    if not any("axis" in line.lower() for line in lines[:50]):
        return None

    # Placeholder - return None to let generic table parser handle it
    return None
