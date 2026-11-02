# Automated Build, Testing and Deployment of a Web Application Using Jenkins, Maven, Ansible and Azure Pipelines

**Student:** Shaik Sameer · MCA, Mohan Babu University · Reg. No. 25102D020019

This project automates the full journey of a web application, from a `git push` to a running, health-checked
deployment, with no manual steps.

| Tool | Role in this project |
|------|----------------------|
| **Maven** | Compiles the code, runs unit tests and integration tests, measures code coverage (JaCoCo) and packages the app as a jar |
| **Jenkins** | Local CI/CD server. Watches the Git repo, runs the Maven stages, publishes the test and coverage reports, then calls Ansible |
| **Ansible** | Deploys the jar to a server (your Mac or an Azure VM), restarts the app, health-checks it, keeps 3 releases and supports rollback |
| **Azure Pipelines** | Cloud CI/CD. The same build, test and deploy flow, triggered from GitHub/Azure Repos, deploying to an Azure VM through Ansible |

The application is **DevOps Task Manager**, a Spring Boot web app with a REST API and a dark glass-style UI.
The page shows the build number that is running, so each pipeline run is visible in the browser.

---

## 1. Architecture

```
 Developer ──git push──► GitHub repo
                            │
          ┌─────────────────┴──────────────────┐
          ▼                                    ▼
   ┌─────────────┐                     ┌────────────────┐
   │   Jenkins   │                     │ Azure Pipelines│
   └─────┬───────┘                     └──────┬─────────┘
         │  Maven: compile → unit tests →     │  Maven@4: clean verify
         │  integration tests → coverage →    │  publish tests + coverage
         │  package taskmanager.jar           │  publish artifact "drop"
         ▼                                    ▼
   ansible-playbook deploy.yml          ansible-playbook deploy.yml
     --limit local                        --limit azure (over SSH)
         │                                    │
         ▼                                    ▼
   App on your Mac :8085               App on Azure VM :8080 (systemd)
         │                                    │
         └──── health check /actuator/health ─┘
```

### Pipeline stages

| # | Stage | Jenkins | Azure Pipelines |
|---|-------|---------|-----------------|
| 1 | Checkout | `checkout scm` | automatic |
| 2 | Build | `mvn clean compile` | `Maven@4` |
| 3 | Unit tests | `mvn test` + JUnit report | `Maven@4` (publishJUnitResults) |
| 4 | Integration tests + coverage gate (≥70%) | `mvn verify -DskipUTs=true` + JaCoCo HTML | `Maven@4` + `PublishCodeCoverageResults@2` |
| 5 | Package | archive `target/taskmanager.jar` | `PublishPipelineArtifact@1` |
| 6 | Deploy | `ansible-playbook --limit local/azure` | `ansible-playbook --limit azure` |
| 7 | Smoke test | `curl /actuator/health` | `curl /actuator/health` |

---

## 2. Project structure

```
Devops Mini Pro/
├── pom.xml                         Maven build: Spring Boot, tests, JaCoCo, build number
├── Jenkinsfile                     Jenkins declarative pipeline
├── azure-pipelines.yml             Azure Pipelines YAML (Build_Test + Deploy stages)
├── src/main/java/com/devops/taskmanager/
│   ├── TaskManagerApplication.java
│   ├── model/        Task.java, TaskRequest.java
│   ├── service/      TaskService.java, TaskNotFoundException.java
│   └── controller/   TaskController.java, InfoController.java, ApiExceptionHandler.java
├── src/main/resources/
│   ├── application.properties
│   └── static/       index.html, style.css, app.js   (the UI)
├── src/test/java/com/devops/taskmanager/
│   ├── service/TaskServiceTest.java        unit tests (7)
│   ├── controller/TaskControllerTest.java  API tests with MockMvc (6)
│   └── ApplicationIT.java                  integration tests on a real server (2)
└── ansible/
    ├── ansible.cfg, inventory.ini          hosts: local (your Mac) and azure (VM)
    ├── group_vars/all.yml, azure.yml       settings per environment
    ├── deploy.yml                          deploy a build
    ├── rollback.yml                        go back to an earlier build
    ├── tasks/switch_and_restart.yml        shared restart + health-check steps
    └── templates/taskmanager.service.j2    systemd service for Linux servers
```

### REST API

| Method | URL | Purpose |
|--------|-----|---------|
| GET | `/api/tasks` | List tasks |
| GET | `/api/tasks/{id}` | One task |
| POST | `/api/tasks` | Create `{ "title": "...", "priority": "HIGH" }` |
| PUT | `/api/tasks/{id}` | Update |
| PATCH | `/api/tasks/{id}/toggle` | Mark done or not done |
| DELETE | `/api/tasks/{id}` | Delete |
| GET | `/api/tasks/stats` | Totals |
| GET | `/api/info` | Version, build number, environment |
| GET | `/actuator/health` | Health check used by Ansible and the pipelines |

---

## 3. Install the tools (macOS)

Run these in Terminal one at a time:

```bash
brew install openjdk@17
brew install maven
brew install ansible
brew install jenkins-lts
```

Check them:

```bash
java -version          # 17 or newer
mvn -v
ansible --version
```

If `java -version` shows an older version, run:
`sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk`

---

## 4. Step 1: Build and test with Maven

From the project folder:

```bash
cd ~/Desktop/"Devops Mini Pro"
mvn clean verify
```

This runs 13 unit tests and 2 integration tests, checks coverage, and builds `target/taskmanager.jar`.
Reports to show in your viva:

- Test results: `target/surefire-reports/` and `target/failsafe-reports/`
- Coverage: open `target/site/jacoco/index.html` in a browser

Run the app yourself:

```bash
java -jar target/taskmanager.jar
```

Open http://localhost:8080.

---

## 5. Step 2: Deploy with Ansible (on your Mac)

```bash
cd ansible
ansible-playbook deploy.yml --limit local -e build_number=1
```

Ansible copies the jar to `~/apps/taskmanager/releases/`, points `current.jar` at it, restarts the app,
waits for `/actuator/health` to report `UP`, and prints the deployed build. Open http://localhost:8085.

Run it again with `-e build_number=2` and `3`, then roll back:

```bash
ansible-playbook rollback.yml --limit local -e rollback_build=2
```

To stop the app: `kill $(cat ~/apps/taskmanager/app.pid)`

---

## 6. Step 3: Jenkins pipeline

1. **Push the project to GitHub** (Jenkins and Azure both pull from it):
   ```bash
   git init
   git add .
   git commit -m "DevOps mini project"
   git branch -M main
   git remote add origin https://github.com/<your-username>/devops-mini-project.git
   git push -u origin main
   ```
2. **Start Jenkins:** `brew services start jenkins-lts`, open http://localhost:8080, unlock it with the
   password from `cat ~/.jenkins/secrets/initialAdminPassword`, and install the suggested plugins.

   > Jenkins also uses port 8080. That is why the locally deployed app uses **8085**.
3. **Add plugins** (Manage Jenkins → Plugins → Available): *HTML Publisher*. *Pipeline*, *Git*, *JUnit*
   and *Timestamper* come with the suggested plugins.
4. **Configure tools** (Manage Jenkins → Tools):
   - JDK → Add JDK → Name `JDK17`, untick "Install automatically", JAVA_HOME = output of
     `/usr/libexec/java_home -v 17`
   - Maven → Add Maven → Name `Maven3`, tick "Install automatically" (or set MAVEN_HOME to `$(brew --prefix maven)/libexec`)
5. **Create the job:** New Item → `devops-mini-project` → *Pipeline* → under Pipeline choose
   *Pipeline script from SCM* → Git → your repo URL → branch `*/main` → Script Path `Jenkinsfile` → Save.
6. Click **Build with Parameters** → DEPLOY_TARGET = `local` → Build.

The stage view shows every stage turning green. Each build has a test result trend, a **JaCoCo Coverage** link, the
archived jar, and the app at http://localhost:8085 showing the new build number. Jenkins polls Git every
5 minutes, so a `git push` starts a new build by itself.

> If the HTML coverage report looks unstyled, run this in Manage Jenkins → Script Console:
> `System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")`

---

## 7. Step 4: Azure Pipelines deploying to an Azure VM

### 7a. Create the VM (Azure for Students gives free credit)

1. Azure Portal → Virtual machines → Create → **Ubuntu Server 22.04 LTS**, size **B1s**.
2. Authentication: **SSH public key**, username `azureuser`, *Generate new key pair*, and download the
   `.pem` file.
3. Inbound ports: allow **SSH (22)**. After creation, go to Networking → *Add inbound port rule* → port **8080**.
4. Note the VM's **public IP**.

Test Ansible from your Mac first:

```bash
mv ~/Downloads/<key>.pem ~/.ssh/azure_vm_key && chmod 600 ~/.ssh/azure_vm_key
# put your VM IP into ansible/inventory.ini, then:
cd ansible
ansible azure -m ping
ansible-playbook deploy.yml --limit azure -e build_number=1
```

Open `http://<VM-IP>:8080`. On the VM, the app runs as a **systemd service**:
`sudo systemctl status taskmanager`.

You can also run the Jenkins job with DEPLOY_TARGET = `azure`.

### 7b. Set up Azure Pipelines

1. Go to https://dev.azure.com → create an organization and a project.
2. **Pipelines → Library → Secure files** → upload the `.pem` key and name it exactly **`azure_vm_key`**.
3. **Pipelines → Environments** → New environment → **`production`** (optional: Approvals and checks → Approvals,
   to get a manual approval before each deploy).
4. **Pipelines → New pipeline** → GitHub → select your repo → *Existing Azure Pipelines YAML file* →
   `/azure-pipelines.yml`.
5. Before running, click **Variables** and add `VM_HOST` = your VM's IP and `VM_USER` = `azureuser`.
6. Run it. On the first run, click **Permit** when it asks for access to the secure file and the environment.

> New Azure DevOps organizations may have no free Microsoft-hosted build minutes. If the run says
> "No hosted parallelism has been purchased or granted", request the free grant at
> https://aka.ms/azpipelines-parallelism-request (it takes 2–3 business days). Until then you can use
> a self-hosted agent on your Mac (Project settings → Agent pools → Default → New agent).

You will see **Build & Test** (with a Tests tab and a Code Coverage tab), then **Deploy to Azure VM**, and the
running app at `http://<VM-IP>:8080` showing the Azure build number.

---

## 8. Demo script for the viva

1. Show the app running and its build number.
2. Change something small (for example the subtitle in `src/main/resources/static/index.html`), then commit and push.
3. Show Jenkins and Azure Pipelines starting on their own, the stages passing, the test and coverage reports.
4. Refresh the app: the change and the new build number are live.
5. **Quality gate:** break a test (for example change `hasSize(3)` to `hasSize(4)` in `TaskControllerTest`)
   and push. The pipeline stops at the test stage and nothing is deployed. Revert it.
6. **Rollback:** `ansible-playbook rollback.yml --limit local -e rollback_build=<earlier build>`.

---

## 9. Troubleshooting

| Problem | Fix |
|---------|-----|
| `Port 8080 already in use` when running the jar | Jenkins is using it. Use `PORT=9090 java -jar target/taskmanager.jar` |
| Jenkins: `ansible-playbook: not found` | The Jenkinsfile adds `/opt/homebrew/bin` to PATH in the Deploy stage. Check `which ansible-playbook` and add that folder to the `withEnv` line in the Jenkinsfile |
| Jenkins: `No such tool JDK17` | The tool names in Manage Jenkins → Tools must be exactly `JDK17` and `Maven3` |
| App stops right after the Jenkins build | Keep `JENKINS_NODE_COOKIE = 'dontKillMe'` in the Jenkinsfile (already set) |
| Build fails with "coverage checks have not been met" | Coverage dropped below 70%. Add tests, or lower `<minimum>` in `pom.xml` |
| Azure deploy: `UNREACHABLE` | Check `VM_HOST`, that port 22 is open, and that the secure file is the right key |
| Browser can't open `http://<VM-IP>:8080` | Add an inbound rule for port 8080 in the VM's Networking settings |
