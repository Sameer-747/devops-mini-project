// Jenkins declarative pipeline: Build -> Unit Test -> Integration Test -> Package -> Deploy (Ansible) -> Smoke Test
// Needs in Manage Jenkins > Tools: a JDK named "JDK17" and a Maven named "Maven3"
// (or delete the tools block if java/mvn are already on the agent's PATH).
pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['local', 'azure'], description: 'Ansible inventory group to deploy to')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Only build and test')
    }

    environment {
        APP_PORT = '8085'
        // Stops Jenkins killing the app that Ansible starts in the background (local target)
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        // Check the Git repository for new commits every 5 minutes
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B test -Dbuild.number=${BUILD_NUMBER}'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests & Coverage') {
            steps {
                // Runs *IT tests, builds the jar and enforces the JaCoCo coverage gate
                sh 'mvn -B verify -DskipUTs=true -Dbuild.number=${BUILD_NUMBER}'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml'
                    publishHTML(target: [
                        reportName : 'JaCoCo Coverage',
                        reportDir  : 'target/site/jacoco',
                        reportFiles: 'index.html',
                        keepAll    : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing: true
                    ])
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'target/taskmanager.jar', fingerprint: true
            }
        }

        stage('Deploy with Ansible') {
            when { expression { !params.SKIP_DEPLOY } }
            steps {
                // Homebrew locations so Jenkins on a Mac can find ansible-playbook
                withEnv(['PATH+HOMEBREW=/opt/homebrew/bin:/usr/local/bin']) {
                  dir('ansible') {
                    sh """
                        ansible-playbook -i inventory.ini deploy.yml \
                            --limit ${params.DEPLOY_TARGET} \
                            -e build_number=${BUILD_NUMBER} \
                            -e jar_source=${WORKSPACE}/target/taskmanager.jar
                    """
                  }
                }
            }
        }

        stage('Smoke Test') {
            when { expression { !params.SKIP_DEPLOY && params.DEPLOY_TARGET == 'local' } }
            steps {
                sh '''
                    curl -fsS http://localhost:${APP_PORT}/actuator/health
                    curl -fsS http://localhost:${APP_PORT}/api/info
                '''
            }
        }
    }

    post {
        success {
            echo "Build #${BUILD_NUMBER} passed. App: http://localhost:${APP_PORT}"
        }
        failure {
            echo "Build #${BUILD_NUMBER} failed. Check the stage logs above."
        }
    }
}
