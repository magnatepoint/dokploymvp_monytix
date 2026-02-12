#!/usr/bin/env python3
"""Test parsers against sample bank files in sample_bank/."""

import logging
import sys
from pathlib import Path

# Reduce log noise
logging.basicConfig(level=logging.WARNING, format="%(message)s")

# Add backend to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.spendsense.etl.parsers import parse_transactions_file

SAMPLE_DIR = Path(__file__).parent.parent / "app/spendsense/sample_bank"


def main():
    results = []

    print("=" * 60)
    print("EXCEL FILES")
    print("=" * 60)
    for f in sorted(SAMPLE_DIR.glob("excel/*")):
        if f.suffix.lower() in (".xls", ".xlsx", ".csv"):
            try:
                records = parse_transactions_file(f.read_bytes(), f.name)
                results.append((f.name, "excel", len(records), None))
                print(f"  {f.name}: {len(records)} records - OK")
            except Exception as e:
                results.append((f.name, "excel", 0, str(e)))
                print(f"  {f.name}: 0 records - FAIL")
                print(f"    Error: {e}")

    print()
    print("=" * 60)
    print("PDF FILES")
    print("=" * 60)
    for f in sorted(SAMPLE_DIR.glob("pdf/*")):
        if f.suffix.lower() == ".pdf":
            try:
                records = parse_transactions_file(f.read_bytes(), f.name)
                results.append((f.name, "pdf", len(records), None))
                print(f"  {f.name}: {len(records)} records - OK")
            except Exception as e:
                results.append((f.name, "pdf", 0, str(e)))
                print(f"  {f.name}: 0 records - FAIL")
                print(f"    Error: {e}")

    print()
    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)
    passed = len([r for r in results if r[3] is None])
    failed = len([r for r in results if r[3] is not None])
    print(f"  Total: {len(results)} files | Passed: {passed} | Failed: {failed}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
