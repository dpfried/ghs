
# GHSearch Platform

This project is made of two subprojects:
1. `application`: The main application has two main responsibilities:
    1. Crawling GitHub and retrieving repository information. This can be disabled with `app.crawl.enabled` argument.
    2. Serving as the backend server for website/frontend
2. `front-end`: A frontend for searching the database (http://seart-ghs.si.usi.ch)

## Setup & Run Project Locally (for development)

The detailed instruction can be find [here](./README_SETUP.md)


## Dockerisation :whale:

GHS service is composed of three containers:

| Service name | Container name | Goal |
| ------------ | -------------- | ---- |
| `gse-app` | `gse-app` | for the spring application itself |
| `gse-fe` | `gse-fe` | for supplying the front end files |
| `gse-db` | `gse-db` | for the database |

The docker-compose configuration `docker-compose.yml` is configured to automatically run scripts to import the previous backup of the database.

### Build and Deploy

1. (only first time) Make sure to initialize GitHub access tokens by updating `V1__initialize_tokens.sql` file and 
   specify the crawler programming languages on `V0__initialize_languages.sql`.
   - This should be done only for the first time. Once the migration is done by Flyway, this file should not be touched.

2. Build the backend as `jar` file: 
```shell
mvn clean package
```

3. To deploy back-end image use the following commands (the tailing `.` should refers to root of the project):
```shell
docker build -t ghs-backend:latest -f docker/Dockerfile.be .
```

4. To deploy front-end images, simply run (the tailing `.` should refers to root of the project):
```shell
docker build -t ghs-frontend:latest -f docker/Dockerfile.fe .
```

5. On server side, or the machine you want to deploy on:
   1. (first time only) Copy `docker-compose` folder on your server.
   2. Fetch new image(s) you just built: `docker-compose pull`
   3. The, run it: `docker-compose up`


## More Info on Flyway and Database Migration
Read [here](./README_flyway.md)

## FAQ
- **How add a new programming language to platform?** See [this commit for adding C#](https://gitlab.reveal.si.usi.ch/devinta/github-search-engine/-/commit/2fd9c1da171119f5d33fd157b2275ad6429264ce) on 17th December 2020.

## Important TODOs
- [ ] Current *Advance Search* re-implementation via Native SQL query may be subject to SQL Injection.
