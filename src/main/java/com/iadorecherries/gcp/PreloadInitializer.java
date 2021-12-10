package com.iadorecherries.gcp;

import com.iadorecherries.gcp.thatorthis.internal.Bootstrap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PreloadInitializer {
    private static final Logger LOGGER = LogManager.getLogger("gcp/initializer");
    // private static final String MOD_FOLDER = MinecraftClient
    //         .getInstance()
    //         .runDirectory
    //         .getAbsolutePath()
    //         + "/mods/";
    private static final String MOD_FOLDER = FabricLoader
            .getInstance()
            .getConfigDir()
            + "/mods/";
    private static final String GCP_FOLDER = MOD_FOLDER + "gcp/";
    private static final String EXPANDED_NAME_REGEX = "(?i)-[0-9].+\\.jar";
    // private static final String DEFAULT_REPO = "Coney-Poney/mc-mods";
    private static final String DEFAULT_REPO = "Coney-Poney/minecraft-mods";

    private @NotNull String trimVersionTag(@NotNull String name) {
        return name.replaceAll(EXPANDED_NAME_REGEX, "");
    }

    private @Nullable Path expandFilename(String filename) {
        Path folder = Paths.get(GCP_FOLDER);

        try {
            Stream<Path> files = Files.list(folder);
            return files.filter(x -> x.getFileName().toString().matches(filename + EXPANDED_NAME_REGEX)
                            || x.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            System.out.println("There was a problem getting files from the mods folder: " + e);
            return null;
        }
    }

    private @Nullable String downloadAndGetFilename(GitHub.GitHubFile metadata) {
        try {
            final URL url = new URL(metadata.url);
            try (InputStream is = url.openStream()) {
                Path path = Paths.get(GCP_FOLDER + metadata.name);
                Files.copy(is, path);
                return path.getFileName().toString();
            } catch (Exception e) {
                System.err.println("There was an error downloading " + metadata.name + ": " + e);
            }
        } catch (IOException e) {
            System.err.println("There was an error downloading " + metadata.name + ": " + e);
        }
        return null;
    }


    public void onInitializing() {
        // create the folder before trying to operate anything on it
        try {
            Files.createDirectories(Paths.get(GCP_FOLDER));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            GitHub.GitHubFile[] fileArray = GitHub.getFiles(DEFAULT_REPO);

            Map<Boolean, List<ImmutablePair<Path, GitHub.GitHubFile>>> fileExists = Arrays.stream(fileArray)
                    .map(g -> {
                        Path f = expandFilename(trimVersionTag(g.name));
                        f = f == null ? Paths.get(GCP_FOLDER + g.name) : f;
                        return new ImmutablePair<>(f, g);
                    })
                    .collect(Collectors.partitioningBy(x -> Files.exists(x.getLeft()) && !Files.isDirectory(x.getLeft())));

            // if the file exists, then attempt to continue with the file under the
            // guise that it will be "updated"
            Map<Boolean, List<ImmutablePair<Path, GitHub.GitHubFile>>> updated = fileExists.get(true).stream()
                    .filter(x -> !x.getRight().name.equals(x.getLeft().getFileName().toString()))
                    .collect(Collectors.partitioningBy(x -> {
                        try {
                            Files.delete(x.getLeft());
                            return true;
                        } catch (IOException e) {
                            System.err.println("Failed to delete "
                                    + x.getLeft().getFileName().toString()
                                    + ": " + e);
                            return false;
                        }
                    }));

            updated.get(true)
                    .forEach(x -> downloadAndGetFilename(x.getRight()));
            //      .filter(Objects::nonNull);
            // TODO: need to add logging for these later
            //         .forEach(GCPState::addUpdated);

            fileExists.get(false)
                    .forEach(x -> downloadAndGetFilename(x.getRight()));
            //      .filter(Objects::nonNull);
            // TODO: need to add logging for these later
            //         .forEach(GCPState::addDownloaded);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, Set<String>> modDirs = new HashMap<String, Set<String>>() {{
            put(GCP_FOLDER, new HashSet<String>());
        }};
        Bootstrap.installInjector(modDirs);
    }
}
