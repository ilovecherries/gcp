package com.iadorecherries.gcp;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

class FileLinks {
    public String self = null;
    public String git = null;
    public String html = null;
}

class FileMetadata {
    public String name = null;
    public String path = null;
    public String sha = null;
    public int size = -1;
    public String url = null;
    public String html_url = null;
    public String git_url = null;
    public String download_url = null;
    public String type = null;
    public FileLinks _links = null;
}

public class GitHub {
    private static final String API_FORMAT = "https://api.github.com/repos/%s/%s";

    public static class GitHubFile {
        public String name = null;
        public String sha = null;
        public String url = null;
    }

    public static GitHubFile[] getFiles(String repoPath) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(API_FORMAT, repoPath, "contents/")))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new Gson();

        FileMetadata[] fileArray = gson.fromJson(response.body(), FileMetadata[].class);
        return Arrays.stream(fileArray)
                .map(f ->
                {
                    var file = new GitHubFile();
                    file.name = f.name;
                    file.sha = f.sha;
                    file.url = f.download_url;
                    return file;
                })
                .toArray(GitHubFile[]::new);
    }
}
