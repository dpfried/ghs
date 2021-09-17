import argparse
import sys

from db import connection

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--columns", nargs="+", default=["name", "stargazers", "main_language", "license"])
    parser.add_argument("--no_forks", action="store_true")
    parser.add_argument("--mit_licensed", action="store_true")
    parser.add_argument("--limit", type=int)

    args = parser.parse_args()

    filter_clauses = []
    if args.no_forks:
        filter_clauses.append("is_fork_project=0")
    if args.mit_licensed:
        filter_clauses.append("license='MIT License'")
    if filter_clauses:
        filter_str = f' WHERE {" AND ".join(filter_clauses)} '
    else:
        filter_str = ''

    limit_str = ' LIMIT {args.limit} ' if args.limit is not None else ''

    query = f"SELECT {', '.join(args.columns)} FROM repo {filter_str} {limit_str}"
    print(query, file=sys.stderr)

    cnx = connection()
    cursor = cnx.cursor()

    cursor.execute(query)
    print(','.join(args.columns))
    for tpl in cursor:
        for s in tpl:
            assert ',' not in str(s)
        print(','.join(str(s) for s in tpl))

    cursor.close()
    cnx.close()
