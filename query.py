from db import connection

cnx = connection()

cursor = cnx.cursor()

def execute(names, query, cursor=cursor):
    print(query)
    cursor.execute(query)
    print('\t'.join(names))
    for tpl in cursor:
        print('\t'.join(str(s) for s in tpl))
    print()

def where_clauses(no_forks=False, language=None):
    where_clauses = []
    if no_forks:
        where_clauses.append("is_fork_project=0")
    if language is not None:
        where_clauses.append(f"main_language='{language}'")
    if bool(where_clauses):
        conjunction = ' AND '.join(where_clauses)
        return f" WHERE  {conjunction} "
    else:
        return ""

def q_count(no_forks=False, language=None):
    q = "SELECT COUNT(*) FROM repo ";
    q += where_clauses(no_forks, language)
    return ["count"], q

def q_license_count(no_forks=False, language=None):
    q = "SELECT license, COUNT(*) AS license_count FROM repo "  
    q += where_clauses(no_forks, language)
    q += "GROUP BY license ORDER BY license_count DESC"
    return ["license", "license_count"], q

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--language")

    args = parser.parse_args()

    print("-" * 10)
    print("including forks")
    print("-" * 10)
    execute(*q_count(language=args.language))
    execute(*q_license_count(language=args.language))

    print("-" * 10)
    print("no forks")
    print("-" * 10)

    execute(*q_count(no_forks=True, language=args.language))
    execute(*q_license_count(no_forks=True, language=args.language))

    cursor.close()
    cnx.close()
