package com.example.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class GitSyncExample {
    public static boolean isSuccess;
    protected static String JBPM_REPO_URL;
    protected static String BACKUP_REPO_URL;

    public static String getJbpmRepoUrl() {
        return JBPM_REPO_URL;
    }

    public static void setJbpmRepoUrl(String jbpmRepoUrl) {
        JBPM_REPO_URL = jbpmRepoUrl;
    }

    public static String getBackupRepoUrl() {
        return BACKUP_REPO_URL;
    }

    public static void setBackupRepoUrl(String backupRepoUrl) {
        BACKUP_REPO_URL = backupRepoUrl;
    }

    public static boolean isIsSuccess() {
        return isSuccess;
    }


    public static String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    public static void main(String[] args) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(".env")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String jbpmRepoUrl = getJbpmRepoUrl()== null ? properties.getProperty("JBPM_REPO_URL"): JBPM_REPO_URL;
        String backupRepoUrl = getBackupRepoUrl()== null ?properties.getProperty("BACKUP_REPO_URL") : BACKUP_REPO_URL;
        String localPath = properties.getProperty("LOCAL_PATH");
        String jbpmGitUsername = properties.getProperty("JBPM_GIT_USERNAME");
        String jbpmGitPasssword = properties.getProperty("JBPM_GIT_PASSWORD");
        String backupToken = properties.getProperty("BACKUP_TOKEN");
        String backupUserName= properties.getProperty("BACKUP_USERNAME");
        String changeFilePath= properties.getProperty("CHANGE_FILE_PATH");
        // /git dizinini kontrol et, sil ve oluştur
        deleteAndCreateDirectory(Path.of(localPath));

        isSuccess = performGitOperations(localPath, jbpmRepoUrl, backupRepoUrl, jbpmGitUsername, jbpmGitPasssword, backupToken,backupUserName,currentDateTime,changeFilePath);

        if (isSuccess) {
            System.out.println("Git operations completed successfully.");
            deleteDirectoryContents(Path.of(localPath)); // İşlem tamamlandıktan sonra dizin içeriğini sil
        } else {
            System.out.println("Git operations failed.");
        }
    }

    public static boolean performGitOperations(String localPath, String jbpmRepoUrl, String targetUrl,
                                               String jbpmGitUsername, String jbpmGitPasssword, String backupToken, String backupUserName, String currentDateTime, String changeFilePath) {
        try {
            // Kaynak depoyu klonla
            Git git = Git.cloneRepository()
                    .setURI(targetUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(backupUserName, backupToken))
                    .call();

            // Hedef repository'den güncel bilgileri çek (pull işlemi)
            // Hedef depo URL'sini ayarla
            git.remoteSetUrl()
                    .setRemoteName("origin")
                    .setRemoteUri(new URIish(jbpmRepoUrl))
                    .call();
            git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(jbpmGitUsername, jbpmGitPasssword)).call();


            // Belirtilen dosyayı bul ve içeriğine ekleme yap
            changeFile(localPath, currentDateTime, changeFilePath);

            // Değişikliği Git'e ekle
            git.add().addFilepattern(".").call();

            if (commit(currentDateTime, git)) return false; // Hiç commit yoksa işlemi durdur
            // Hedef depo URL'sini ayarla
            git.remoteSetUrl()
                    .setRemoteName("second")
                    .setRemoteUri(new URIish(targetUrl))
                    .call();

            Iterable<PushResult> pushResults = getPushResults(git.push().setRemote("second"), backupUserName, backupToken);

            if (pushControl(localPath, targetUrl, backupToken, backupUserName, currentDateTime, pushResults, git,"Push successful to "))
            {
                try {
                    git.add().addFilepattern(".").call();
                    if (commit(currentDateTime, git)) return false; // Hiç commit yoksa işlemi durdur
                    else pushResults = getPushResults(git.push().setRemote("second"), backupUserName, backupToken);
                    if (pushControl(localPath, jbpmRepoUrl, backupToken, backupUserName, currentDateTime, pushResults, git,"Push successful to ")) {
                        getPushResults(git.push().setRemote("origin"), jbpmGitUsername, jbpmGitPasssword);
                        return true;
                    }
                    else return false;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else return false; // Hiç commit yoksa işlemi durdur

          }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void changeFile(String localPath, String currentDateTime, String changeFilePath) {
        String filePath = localPath +"/"+ changeFilePath;
        appendToFile(filePath, "\n " +
                "rule \"deneme " + currentDateTime + "\"\n" +
                "when\n" +
                "then\n" +
                "end ");
    }

    private static boolean pushControl(String localPath, String toUrl, String backupToken, String backupUserName, String currentDateTime, Iterable<PushResult> pushResults, Git git, String pushSuccessfulTo) throws GitAPIException {
        for (PushResult result : pushResults) {
            for (RemoteRefUpdate update : result.getRemoteUpdates()) {
                if (update.getStatus() == RemoteRefUpdate.Status.OK) {
                    System.out.println("Push successful for ref: " + update.getRemoteName());
                    writeCommitLog(localPath, currentDateTime, pushSuccessfulTo+" ->" + toUrl);
                    // Değişikliği Git'e ekle
                    git.add().addFilepattern(".").call();
                    if(commit(currentDateTime, git)) return true;
                    return true;
                } else {
                    System.out.println("Push failed for ref: " + update.getRemoteName() + ", status: " + update.getStatus());
                    if (update.getStatus() == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                        System.out.println("Push rejected because of non-fast-forward. Attempting to pull and re-push...");

                        git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(backupUserName, backupToken)).call();
                        git.add().addFilepattern(".").call();
                        if(commit(currentDateTime, git)) return true;
                        Iterable<PushResult> retryPushResults = getPushResults(git.push(), backupUserName, backupToken);

                        for (PushResult retryResult : retryPushResults) {
                            for (RemoteRefUpdate retryUpdate : retryResult.getRemoteUpdates()) {
                                if (retryUpdate.getStatus() == RemoteRefUpdate.Status.OK) {
                                    System.out.println("Push successful after pull for ref: " + retryUpdate.getRemoteName());
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Iterable<PushResult> getPushResults(PushCommand git, String backupUserName, String backupToken) throws GitAPIException {
        // Hedef depoya push et
        Iterable<PushResult> pushResults = git
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(backupUserName, backupToken))
                .call();
        return pushResults;
    }

    private static boolean commit(String currentDateTime, Git git) throws GitAPIException {
        // Commit işlemi ve tarih ekleme
        // Commit edilmemiş değişiklikler varsa commit et
        if (!git.status().call().isClean()) {
            git.commit()
                    .setMessage("Automatic commit for added changes")
                    .call();
            System.out.println("Changes committed.");
        } else {
            System.out.println("No changes to commit.");
        }
        //commit
        git.commit().setMessage("Automatic commit on " + currentDateTime).call();
        // Commit var mı kontrol et
        if (!hasCommits(git)) {
            System.out.println("No commits found in the source repository.");
            return true;
        }
        return false;
    }

    public static void deleteAndCreateDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a)) // Tersine sırala
                        .map(Path::toFile)
                        .forEach(File::delete);
                Files.deleteIfExists(path); // Ana dizini de sil
            }
            Files.createDirectories(path); // Dizin oluştur
        } catch (Exception e) {
            System.out.println("Error while deleting and creating directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void appendToFile(String filePath, String content) {
        try {
            Files.write(Paths.get(filePath), (content + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            System.out.println("Content appended to " + filePath);
        } catch (IOException e) {
            System.out.println("Error while appending to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean hasCommits(Git git) {
        try {
            return git.log().call().iterator().hasNext();
        } catch (GitAPIException e) {
            System.out.println("Error while checking commits: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void deleteDirectoryContents(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .filter(p -> !p.equals(path)) // Ana dizini filtrele
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            System.out.println("Error while deleting directory contents: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writeCommitLog(String localPath, String dateTime, String commitMessage) {
        String logFilePath = localPath + "/"+"commit_log.txt";
        try {
            Path path = Path.of(logFilePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path); // Dizin oluştur
            }
        } catch (Exception e) {
            System.out.println("Error create : "+ localPath+" ->" + e.getMessage());
            e.printStackTrace();
        }
        try (FileWriter logWriter = new FileWriter(logFilePath, true)) {
            logWriter.write("Date: " + dateTime + ", Message: " + commitMessage + "\n");
        } catch (IOException e) {
            System.out.println("Error while writing commit log: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
