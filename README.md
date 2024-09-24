# jbpm-work-item-handler-git-backup
jbpm work-item-handler-git-backup

    
        String jbpmRepoUrl = getJbpmRepoUrl().isEmpty() ? properties.getProperty("JBPM_REPO_URL"): JBPM_REPO_URL;
        String backupRepoUrl = getBackupRepoUrl().isEmpty()?properties.getProperty("BACKUP_REPO_URL") : BACKUP_REPO_URL;
        String localPath = properties.getProperty("LOCAL_PATH");
        String jbpmGitUsername = properties.getProperty("JBPM_GIT_USERNAME");
        String jbpmGitPasssword = properties.getProperty("JBPM_GIT_PASSWORD");
        String backupToken = properties.getProperty("BACKUP_TOKEN");
        String backupUserName= properties.getProperty("BACKUP_USERNAME");
        String changeFilePath= properties.getProperty("CHANGE_FILE_PATH"); 
        ...

        git.remoteSetUrl()
          .setRemoteName("origin")
          .setRemoteUri(new URIish(jbpmRepoUrl))
          .call();
        git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(jbpmGitUsername, jbpmGitPasssword)).call();



````
@Wid(widfile="MyWIHGitDefinitions.wid", name="MyWIHGitDefinitions",
        displayName="MyWIHGitDefinitions",
        defaultHandler="mvel: new com.example.git.MyWIHGitWorkItemHandler()",
        documentation = "mywihgit/index.html",
        category = "mywihgit",
        icon = "MyWIHGitDefinitions.png",
        parameters={
            @WidParameter(name="JBPM_REPO_URL"),
            @WidParameter(name="BACKUP_REPO_URL")
        },
        results = {
                @WidResult(name = "Result", runtimeType = "java.lang.Object")
        },
        mavenDepends = {
                @WidMavenDepends(group = "${groupId}", artifact = "${artifactId}", version = "${version}")
        },
        serviceInfo = @WidService(category = "${name}", description = "${description}",
                keywords = "git,pull, push",
                action = @WidAction(title = "Git Pull Push"),
                authinfo = @WidAuth(
                        referencesite = "https://github.com/remzisahbaz")
        )
)
````
![image](https://github.com/user-attachments/assets/4519236b-6a2c-477c-94c8-784ae01b10dc)

