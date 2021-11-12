# Installation and Usage

**Table of Content**:
1. [Install Dependencies](#0-install-dependencies)
2. [Setup MySQL](#1-setup-mysql)
3. [Setup Crawler](#2-setup-crawler)
4. [Running the `application`](#3-running-the-application)
   1. Running in IntelliJ
   2. Running in the terminal
   3. Running using `.jar`
   4. [Supported arguments](#supported-arguments)
---

## 1. Install Deps

Install JDK if it's not installed (it should be already installed on devfairs):
```sudo apt-get install openjdk-11-jdk```

Create ~/.my.cnf with the following:

```
[mysqld]
default_time_zone = "+00:00"
group_concat_max_len=10000
```

Install mysql:

### Option 1 (with conda; run this inside a conda env)
```
conda install -c conda-forge mysql-server mysql-client mysql
conda install mysql-connector-python
mysqld --initialize-insecure --user=mysql --basedir=$CONDA_PREFIX/mysql --datadir=$CONDA_PREFIX/mysql/data
mysqld --basedir=$CONDA_PREFIX/mysql --datadir=$CONDA_PREFIX/mysql/data
```

The mysql demon should now be running inside a terminal; open a new one to carry out the rest of the steps.

### Option 2 (with root permissions)
```
sudo apt install mysql-server
sudo mysql_secure_installation
sudo service mysql start
```

## 2. Setup MySQL

For the project to work, one must first create the necessary user and DB table specified in the <code>application.properties</code>, and grant the user access and modification privileges to said DB table.

### Step 1/3: Create Database: `gse`

Start mysql:
```
mysql -u root
```
or if you used option 2, you may need to run `sudo mysql` instead.

Then create the database:

```mysql
CREATE DATABASE gse CHARACTER SET utf8 COLLATE utf8_bin;
```

### Step 2/3: Create User: `gseadmin`

Create the user by running these two commands in sequence:  
```mysql
CREATE USER 'gseadmin'@'%' identified by 'Lugano2020';
GRANT ALL ON gse.* to 'gseadmin'@'%';
```

**Note**: The `gseadmin` user is only required for the flyway migrations, as well as for JPA to access the database.


### Step 3/3: Create Tables
Create tables:
```shell
$ mysql -u gseadmin -pLugano2020 gse < ./docker-compose/initdb/1-gse-db-schema.sql
```

## 2. Setup Crawler

1. Create a github API personal access token using [these instructions](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token). The API permissions I granted are `public_repo, read:discussion, read:org, read:repo_hook, repo:status, repo_deployment, security_events`; but it may work with a subset of these too.

2. Edit `initialize_tokens_languages.sql`, putting your github API personal access token in the final line as the value for `access_token`.

3. Run :
```
mysql -u gseadmin -pLugano2020 gse < initialize_tokens_languages.sql
```

   
## 3. Running the `application`

### II. Running in the terminal

1. Make sure Apache Maven (`mvn`) is installed

    <details>
    <summary>How to install Maven?</summary>
    
    1. First downloaded the latest version of [Apache Maven](https://maven.apache.org/download.cgi).
    2. Next, add the `apache-maven-X.X.X/bin` to `PATH` environment variable
       ```shell
       # add this to ~/.zshrc or ~/.bash_profile
       export PATH="/usr/local/apache-maven-x.x.x/bin/:$PATH"
       ```
    3. To ensure that the path variable has been added, run: `mvn -v`
    </details>

2. Navigate to the root folder of the project. Run, passing start and end dates (e.g. 2020-01-01 and 2020-12-31) as follows:
    ```shell
    mvn spring-boot:run -Dspring-boot.run.arguments=\
        --app.crawl.startdate_override=true,\
        --app.crawl.startdate_override_value=2020-01-01,\
        --app.crawl.enddate_override=true,\
        --app.crawl.enddate_override_value=2020-12-31
    ```
    If port 8080 is already in use, you will get an error about tomcat being able to startup. You can change the port by also adding `--server.port=<port>`.
