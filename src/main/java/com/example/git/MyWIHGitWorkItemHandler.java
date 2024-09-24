/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.git;

import java.util.HashMap;
import java.util.Map;
import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;
import org.jbpm.process.workitem.core.util.RequiredParameterValidator;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.jbpm.process.workitem.core.util.Wid;
import org.jbpm.process.workitem.core.util.WidParameter;
import org.jbpm.process.workitem.core.util.WidResult;
import org.jbpm.process.workitem.core.util.service.WidAction;
import org.jbpm.process.workitem.core.util.service.WidAuth;
import org.jbpm.process.workitem.core.util.service.WidService;
import org.jbpm.process.workitem.core.util.WidMavenDepends;

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
public class MyWIHGitWorkItemHandler extends AbstractLogOrThrowWorkItemHandler {
        private String JBPM_REPO_URL;
        private String BACKUP_REPO_URL;

    public MyWIHGitWorkItemHandler(String JBPM_REPO_URL, String BACKUP_REPO_URL){
            this.JBPM_REPO_URL = this.JBPM_REPO_URL;
            this.BACKUP_REPO_URL = this.BACKUP_REPO_URL;
        }

    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        try {
            RequiredParameterValidator.validate(this.getClass(), workItem);

            // sample parameters
            JBPM_REPO_URL = (String) workItem.getParameter("JBPM_REPO_URL");
            BACKUP_REPO_URL = (String) workItem.getParameter("BACKUP_REPO_URL");

            // return results
           // GitSyncExample.setJbpmRepoUrl(JBPM_REPO_URL);
         //   GitSyncExample.setBackupRepoUrl(BACKUP_REPO_URL);
            String sampleResult = "result : ";// + GitSyncExample.isIsSuccess();
            Map<String, Object> results = new HashMap<String, Object>();
            results.put("SampleResult", sampleResult);


            manager.completeWorkItem(workItem.getId(), results);
        } catch(Throwable cause) {
            handleException(cause);
        }
    }

    @Override
    public void abortWorkItem(WorkItem workItem,
                              WorkItemManager manager) {
        // stub
    }
}


