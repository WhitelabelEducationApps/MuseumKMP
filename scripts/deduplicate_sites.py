#!/usr/bin/env python3
"""
Identifies and removes near-duplicate heritage site entries from the database.

Duplicates arise from ASCII-stripped names vs proper Unicode names for the same site.
The script keeps the entry with richer data (more translations, longer descriptions,
better coordinates) and deletes the other.

Usage:
    python scripts/deduplicate_sites.py                    # dry run (default)
    python scripts/deduplicate_sites.py --apply            # actually delete duplicates
    python scripts/deduplicate_sites.py --threshold 0.95   # custom similarity threshold
"""

import sqlite3
import argparse
import shutil
import os
import sys
import io
from difflib import SequenceMatcher
import unicodedata
from datetime import datetime

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

DB_PATH = "androidApp/src/main/assets/heritage_sites.db"

def normalize_for_comparison(s):
    if not s:
        return ""
    s = unicodedata.normalize('NFKD', s)
    s = ''.join(c for c in s if not unicodedata.combining(c))
    return s.strip().lower()


def compute_completeness(row, all_columns):
    score = 0
    for i, val in enumerate(row):
        if val is not None and str(val).strip():
            score += 1
    return score


def pick_keeper(row_a, row_b, columns):
    id_a, id_b = row_a[0], row_b[0]
    name_idx = columns.index('paintingname')
    desc_idx = columns.index('description')
    loc_idx = columns.index('location')

    score_a = compute_completeness(row_a, columns)
    score_b = compute_completeness(row_b, columns)

    name_a = row_a[name_idx] or ""
    name_b = row_b[name_idx] or ""
    has_unicode_a = name_a != normalize_for_comparison(name_a)
    has_unicode_b = name_b != normalize_for_comparison(name_b)

    desc_a = len(str(row_a[desc_idx] or ""))
    desc_b = len(str(row_b[desc_idx] or ""))

    loc_a = str(row_a[loc_idx] or "")
    loc_b = str(row_b[loc_idx] or "")
    loc_precision_a = len(loc_a)
    loc_precision_b = len(loc_b)

    reasons = []

    # Prefer proper Unicode name
    if has_unicode_b and not has_unicode_a:
        reasons.append(f"unicode name ('{name_b}' vs '{name_a}')")
        score_b += 10
    elif has_unicode_a and not has_unicode_b:
        reasons.append(f"unicode name ('{name_a}' vs '{name_b}')")
        score_a += 10

    # Prefer longer description
    if desc_b > desc_a:
        reasons.append(f"longer description ({desc_b} vs {desc_a})")
        score_b += 5
    elif desc_a > desc_b:
        reasons.append(f"longer description ({desc_a} vs {desc_b})")
        score_a += 5

    # Prefer more precise coordinates
    if loc_precision_b > loc_precision_a:
        reasons.append(f"more precise coordinates")
        score_b += 3
    elif loc_precision_a > loc_precision_b:
        reasons.append(f"more precise coordinates")
        score_a += 3

    if score_b >= score_a:
        return id_b, id_a, reasons
    else:
        return id_a, id_b, reasons


def find_duplicates(conn, threshold):
    c = conn.cursor()
    c.execute("PRAGMA table_info(museum_item)")
    columns = [col[1] for col in c.fetchall()]

    col_list = ', '.join(columns)
    c.execute(f"SELECT {col_list} FROM museum_item ORDER BY paintingname")
    rows = c.fetchall()

    name_idx = columns.index('paintingname')

    duplicates = []
    seen = set()

    for i in range(len(rows)):
        norm_i = normalize_for_comparison(rows[i][name_idx])
        if not norm_i:
            continue
        for j in range(i + 1, len(rows)):
            norm_j = normalize_for_comparison(rows[j][name_idx])
            if not norm_j:
                continue

            # Quick length check to skip obviously different names
            if abs(len(norm_i) - len(norm_j)) > max(len(norm_i), len(norm_j)) * 0.1:
                continue

            ratio = SequenceMatcher(None, norm_i, norm_j).ratio()
            if ratio >= threshold:
                id_i, id_j = rows[i][0], rows[j][0]
                key = (min(id_i, id_j), max(id_i, id_j))
                if key in seen:
                    continue
                seen.add(key)

                # Verify they're actually the same site (same country or very high similarity)
                author_idx = columns.index('author')
                author_i = normalize_for_comparison(rows[i][author_idx])
                author_j = normalize_for_comparison(rows[j][author_idx])

                # If names are very similar but countries differ, skip (e.g. Iguazu AR vs Iguaçu BR)
                if ratio < 1.0 and author_i != author_j:
                    author_ratio = SequenceMatcher(None, author_i, author_j).ratio()
                    if author_ratio < 0.8:
                        continue

                keeper_id, delete_id, reasons = pick_keeper(rows[i], rows[j], columns)
                duplicates.append({
                    'keeper_id': keeper_id,
                    'delete_id': delete_id,
                    'name_a': rows[i][name_idx],
                    'name_b': rows[j][name_idx],
                    'id_a': rows[i][0],
                    'id_b': rows[j][0],
                    'ratio': ratio,
                    'reasons': reasons,
                })

    return duplicates


def main():
    parser = argparse.ArgumentParser(description='Deduplicate heritage sites in the database')
    parser.add_argument('--apply', action='store_true', help='Actually delete duplicates (default is dry run)')
    parser.add_argument('--threshold', type=float, default=0.97, help='Similarity threshold (default: 0.97)')
    args = parser.parse_args()

    if not os.path.exists(DB_PATH):
        print(f"Database not found at {DB_PATH}")
        sys.exit(1)

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT COUNT(*) FROM museum_item")
    total_before = c.fetchone()[0]
    print(f"Total entries before: {total_before}")
    print(f"Similarity threshold: {args.threshold}")
    print(f"Mode: {'APPLY' if args.apply else 'DRY RUN'}\n")

    duplicates = find_duplicates(conn, args.threshold)

    if not duplicates:
        print("No duplicates found.")
        conn.close()
        return

    print(f"Found {len(duplicates)} duplicate pairs:\n")
    for d in duplicates:
        marker_a = "KEEP" if d['keeper_id'] == d['id_a'] else "DELETE"
        marker_b = "KEEP" if d['keeper_id'] == d['id_b'] else "DELETE"
        print(f"  [{d['ratio']:.2f}] ID {d['id_a']} [{marker_a}]: '{d['name_a']}'")
        print(f"         ID {d['id_b']} [{marker_b}]: '{d['name_b']}'")
        if d['reasons']:
            print(f"         Keeping because: {', '.join(d['reasons'])}")
        print()

    if args.apply:
        backup_path = DB_PATH + f".backup-{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        shutil.copy2(DB_PATH, backup_path)
        print(f"Backup saved to: {backup_path}")

        delete_ids = [d['delete_id'] for d in duplicates]
        for did in delete_ids:
            c.execute("DELETE FROM museum_item WHERE id = ?", (did,))

        conn.commit()
        c.execute("SELECT COUNT(*) FROM museum_item")
        total_after = c.fetchone()[0]
        print(f"\nDeleted {len(delete_ids)} duplicate entries.")
        print(f"Total entries after: {total_after}")
    else:
        print("--- DRY RUN --- No changes made. Use --apply to delete duplicates.")

    conn.close()


if __name__ == '__main__':
    main()
