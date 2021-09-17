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

def q_count(no_forks=False):
    q = "SELECT COUNT(*) FROM repo ";
    if no_forks:
        q += " WHERE is_fork_project=0 " 
    return ["count"], q

def q_license_count(no_forks=False):
    q = "SELECT license, COUNT(*) AS license_count FROM repo "  
    if no_forks:
        q += " WHERE is_fork_project=0 " 
    q += "GROUP BY license ORDER BY license_count DESC"
    return ["license", "license_count"], q

print("-" * 10)
print("including forks")
print("-" * 10)
execute(*q_count())
execute(*q_license_count())

print("-" * 10)
print("no forks")
print("-" * 10)

execute(*q_count(no_forks=True))
execute(*q_license_count(no_forks=True))

cursor.close()
cnx.close()
