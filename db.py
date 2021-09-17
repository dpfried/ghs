import mysql.connector

def connection():
    return mysql.connector.connect(
        user='gseadmin',
        password='Lugano2020',
        host='127.0.0.1',
        database='gse'
    )

