# Installation and Usage

**Table of Content**:
1. [Setup MySQL](#1-setup-mysql)
2. [Setup Crawler](#2-setup-crawler)
3. [Running the `application`](#3-running-the-application)
   1. Running in IntelliJ
   2. Running in the terminal
   3. Running using `.jar`
   4. [Supported arguments](#supported-arguments)
4. [Running the `frontend`](#4-running-the-frontend)

---

## 0. Install Deps

Install JDK:
```sudo apt-get install openjdk-11-jdk```

Install mysql:

```
sudo apt install mysql-server
sudo mysql_secure_installation
sudo service mysql start
```


## 1. Setup MySQL

For the project to work, one must first create the necessary user and DB table specified in the <code>application.properties</code>, and grant the user access and modification privileges to said DB table.

Create ~/.my.cnf with the following:

```
[mysqld]
default_time_zone = "+00:00"
group_concat_max_len=10000
```

Be sure to restart your MySQL service for the changes to take effect!

```
sudo service mysql stop
sudo service mysql start
```

### Step 2/5: Create Database: `gse`

Create the database for the project by running:
``` mysql
CREATE DATABASE gse CHARACTER SET utf8 COLLATE utf8_bin;
```

### Step 3/5: Create User: `gseadmin`

Create the user by running these two commands in sequence:  
``` mysql
CREATE USER 'gseadmin'@'%' identified by 'Lugano2020';
GRANT ALL ON gse.* to 'gseadmin'@'%';
```

**Note**: The `gseadmin` user is only required for the flyway migrations, as well as for JPA to access the database.


### Step 4/5: Create Tables
Create tables:
```shell
$ mysql -u gseadmin -pLugano2020 gse < ./docker-compose/initdb/1-gse-db-schema.sql
```

### Step 5/5: Populate Tables (Optional)
Initialize the database with an existing dataset of mined repositories — or otherwise the Crawler will start from scratch.
```shell
$ mysql -u gseadmin -pLugano2020 gse < ./docker-compose/initdb/2-gse-db-data-***.sql

## 2. Setup Crawler
To make Crawler work, you have to initialize `supported_language` and  `access_token`. For that, you have two options:

Edit initialize_tokens_languages.sql, adding your github API personal access token obtained using [these instructions](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token).

The permissions I granted are `public_repo, read:discussion, read:org, read:repo_hook, repo:status, repo_deployment, security_events`; but it may work with a subset of these too.
   
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

2. Navigate to the root folder of the project. To run the application with the default parameters (specified in the `application.properties` file), simply run:
    ```shell
    mvn spring-boot:run
    ```
3. And to override the value of an existing parameter, run:
    ```shell
    mvn spring-boot:run -Dspring-boot.run.arguments=--arg.one.name=argvalue,--arg.two.name=1
    ```

### Supported arguments

Here's a list of arguments supported by the application that you can find in `application.properties` (usual place for _Spring_ projects):

| variable name | type | default value | description |
| ------------- | ---- | ------------- | ----------- |
|`app.crawl.enabled`|boolean|true|Specifies if the crawling jobs are enabled on startup|
| `app.crawl.scheduling` | String | 21600000 (6h, in ms) | Scheduling rate, expressed as a numeric string |
| `app.crawl.startdate` | String | 2008-01-01T00:00:00 | "Beginning of time". Basically the earliest supported date for crawling repos, if no crawl jobs were previously performed. Formatted as a yyyy-MM-ddTHH:MM:SS string. |
  
Note that although there are other parameters, I strongly recommend you **DON'T** override them.


## 4. Running the `front-end`

The easiest way to start the front-end is through IntelliJ itself. After starting the application back-end, navigate to `src/main/fe-src` in the project tree. Right click on `index.html`, and select one of the provided launch options from `Open In Browser`. Please note that IntelliJ's built-in web-server port is [by default configured](https://www.jetbrains.com/help/idea/php-built-in-web-server.html#configuring-built-in-web-server) to `63342`. In order to access the back-end API, change it to `3030` in `Preferences > Build, Execution, Deployment > Debugger > Built-in server`, as the application CORS configurer can only accept connections from `localhost:3030`.
