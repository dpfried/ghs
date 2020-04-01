package com.dabico.gseapp.job;

import com.dabico.gseapp.converter.GitRepoConverter;
import com.dabico.gseapp.github.GitHubApiService;
import com.dabico.gseapp.model.GitRepo;
import com.dabico.gseapp.model.GitRepoLabel;
import com.dabico.gseapp.model.GitRepoLanguage;
import com.dabico.gseapp.repository.*;
import com.dabico.gseapp.service.GitRepoService;
import com.dabico.gseapp.util.interval.DateInterval;
import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.*;
import org.javatuples.Pair;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.google.gson.JsonParser.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrawlProjectsJob {

    static final Logger logger = LoggerFactory.getLogger(CrawlProjectsJob.class);

    List<DateInterval> requestQueue = new ArrayList<>();
    List<String> accessTokens = new ArrayList<>();
    List<String> languages = new ArrayList<>();

    int tokenOrdinal;
    String currentToken;

    AccessTokenRepository accessTokenRepository;
    SupportedLanguageRepository supportedLanguageRepository;

    GitRepoConverter gitRepoConverter;

    GitHubApiService gitHubApiService;
    GitRepoService gitRepoService;

    @Autowired
    public CrawlProjectsJob(AccessTokenRepository accessTokenRepository,
                            SupportedLanguageRepository supportedLanguageRepository,
                            GitRepoConverter gitRepoConverter,
                            GitHubApiService gitHubApiService,
                            GitRepoService gitRepoService){
        this.accessTokenRepository = accessTokenRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.gitRepoConverter = gitRepoConverter;
        this.gitHubApiService = gitHubApiService;
        this.gitRepoService = gitRepoService;
    }

    public void run(DateInterval createInterval, DateInterval updateInterval) throws Exception {
        refresh();
        if (createInterval != null){
            create(createInterval);
        }
        if (updateInterval != null){
            update(updateInterval);
        }
    }

    private void create(DateInterval interval) throws Exception {
        for (String language : languages){
            requestQueue.add(interval);
            do {
                DateInterval first = requestQueue.remove(0);
                retrieveRepos(first,language,false);
            } while (!requestQueue.isEmpty());
        }
    }

    private void update(DateInterval interval) throws Exception {
        for (String language : languages){
            requestQueue.add(interval);
            do {
                DateInterval first = requestQueue.remove(0);
                retrieveRepos(first,language,true);
            } while (!requestQueue.isEmpty());
        }
    }

    private void retrieveRepos(DateInterval interval, String language, Boolean update) throws Exception {
        logger.info("Crawling: "+language.toUpperCase()+" "+interval);
        logger.info("Token: " + this.currentToken);
        int page = 1;
        replaceTokenIfExpired();
        Response response = gitHubApiService.searchRepositories(language,interval,page,currentToken,update);
        ResponseBody responseBody = response.body();

        if (response.isSuccessful() && responseBody != null){
            JsonObject bodyJson = parseString(responseBody.string()).getAsJsonObject();
            int totalResults = bodyJson.get("total_count").getAsInt();
            int totalPages = (int) Math.ceil(totalResults/100.0);
            logger.info("Retrieved results: "+totalResults);
            if (totalResults <= 1000){
                JsonArray results = bodyJson.get("items").getAsJsonArray();
                response.close();
                saveRetrievedRepos(results);

                if (totalPages > 1){
                    page++;
                    while (page <= totalPages){
                        replaceTokenIfExpired();
                        response = gitHubApiService.searchRepositories(language,interval,page,currentToken,update);
                        responseBody = response.body();
                        if (response.isSuccessful() && responseBody != null){
                            bodyJson = parseString(responseBody.string()).getAsJsonObject();
                            response.close();
                            results = bodyJson.get("items").getAsJsonArray();
                            saveRetrievedRepos(results);
                            page++;
                        }
                        response.close();
                    }
                }
            } else {
                Pair<DateInterval,DateInterval> newIntervals = interval.splitInterval();
                if (newIntervals != null){
                    requestQueue.add(newIntervals.getValue0());
                    requestQueue.add(newIntervals.getValue1());
                }
                response.close();
            }
        }
        response.close();
        if ((!requestQueue.isEmpty())) {
            logger.info("Next Crawl Intervals:");
            logger.info(requestQueue.toString());
        } else {
            logger.info("Crawl Complete!");
        }
    }

    private void saveRetrievedRepos(JsonArray results) throws Exception {
        logger.info("Adding: "+results.size()+" results");
        for (JsonElement element : results){
            JsonObject repoJson = element.getAsJsonObject();
            GitRepo repo = gitRepoConverter.jsonToGitRepo(repoJson);
            repo = gitRepoService.createOrUpdateRepo(repo);
            retrieveRepoLabels(repo);
            retrieveRepoLanguages(repo);
        }
    }

    private void retrieveRepoLabels(GitRepo repo) throws Exception {
        Response response = gitHubApiService.searchRepoLabels(repo.getName(),currentToken);
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() && responseBody != null){
            JsonArray results = parseString(responseBody.string()).getAsJsonArray();
            results.forEach(result ->
                    gitRepoService.createOrUpdateLabel(GitRepoLabel.builder()
                            .repo(repo)
                            .label(result.getAsJsonObject().get("name").getAsString())
                            .build())
            );
        }
        response.close();
    }

    private void retrieveRepoLanguages(GitRepo repo) throws Exception {
        Response response = gitHubApiService.searchRepoLanguages(repo.getName(),currentToken);
        ResponseBody responseBody = response.body();
        if (response.isSuccessful() && responseBody != null){
            JsonObject result = parseString(responseBody.string()).getAsJsonObject();
            Set<String> keySet = result.keySet();
            keySet.forEach(key ->
                    gitRepoService.createOrUpdateLanguage(GitRepoLanguage.builder()
                            .repo(repo)
                            .language(key)
                            .sizeOfCode(result.get(key).getAsLong())
                            .build())
            );
        }
        response.close();
    }

    private void refresh(){
        getLanguagesToMine();
        getAccessTokens();
        this.tokenOrdinal = -1;
        this.currentToken = getNewToken();
    }

    private void getLanguagesToMine(){
        supportedLanguageRepository.findAll().forEach(language -> languages.add(language.getName()));
    }

    private void getAccessTokens(){
        accessTokenRepository.findAll().forEach(accessToken -> accessTokens.add(accessToken.getValue()));
    }

    private void replaceTokenIfExpired() throws Exception {
        if (gitHubApiService.isTokenLimitExceeded(currentToken)){
            currentToken = getNewToken();
        }
    }

    private String getNewToken(){
        tokenOrdinal++;
        return accessTokens.get(tokenOrdinal % accessTokens.size());
    }
}
