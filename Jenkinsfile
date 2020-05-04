pipeline {

    agent {
        // label "" also could have been 'agent any' - that has the same meaning.
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        APP_NAME = "pet-battle-api"

        JENKINS_TAG = "${JOB_NAME}.${BUILD_NUMBER}".replace("/", "-")
        JOB_NAME = "${JOB_NAME}".replace("/", "-")

        GIT_SSL_NO_VERIFY = true
        GIT_CREDENTIALS = credentials("${PIPELINES_NAMESPACE}-git-auth")
        NEXUS_CREDS = credentials("${PIPELINES_NAMESPACE}-nexus-password")
        ARGOCD_CREDS = credentials("${PIPELINES_NAMESPACE}-argocd-token")
        NEXUS_REPO_NAME="labs-static"
    }

    // The options directive is for configuration that applies to the whole job.
    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
        timeout(time: 15, unit: 'MINUTES')
        ansiColor('xterm')
        timestamps()
    }

    stages {
        stage("prepare environment for master deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /(.*master)/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "labs-dev"
                    env.E2E_TEST_ROUTE = "oc get route/${APP_NAME} --template='{{.spec.host}}' -n ${PROJECT_NAMESPACE}".execute().text.minus("'").minus("'")
                }
            }
        }
        stage("prepare environment for develop deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /(.*develop)/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "labs-dev"
                    env.E2E_TEST_ROUTE = "oc get route/${APP_NAME} --template='{{.spec.host}}' -n ${PROJECT_NAMESPACE}".execute().text.minus("'").minus("'")
                }
            }
        }

        stage("ArgoCD Create App") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { BUILD_NUMBER == 1 }
            }
            steps {
                echo '### Create ArgoCD App ? ###'
                sh '''
                    echo "TODO"
                '''
            }
        }
        stage("Build (Compile App)") {
            agent {
                node {
                    label "jenkins-slave-mvn"
                }
            }
            steps {
                git url: "https://github.com/eformat/pet-battle-api.git"

                echo '### set package versions ###'
                def VERSION = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
                env.PACKAGE = "${APP_NAME}-${VERSION}-${JENKINS_TAG}.tar.gz"
                sh 'printenv'

                echo '### Running tests ###'
                // sh 'mvn test'

                echo '### Running build ###'
                sh '''                    
                    mvn package -DskipTests
                '''

                echo '### Packaging App for Nexus ###'
                sh '''
                    # PACKAGE=${APP_NAME}-${VERSION}-${JENKINS_TAG}.tar.gz                    
                    tar -zcvf ${PACKAGE} Dockerfile.jvm target/lib target/*-runner.jar                     
                    curl -vvv -u ${NEXUS_CREDS} --upload-file ${PACKAGE} http://${NEXUS_SERVICE_SERVICE_HOST}:${NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE}
                '''
            }
            // Post can be used both on individual stages and for the entire build.
        }

        stage("Bake (OpenShift Build)") {
            agent {
                node {
                    label "master"
                }
            }
            steps {
                echo '### Get Binary from Nexus and shove it in a box ###'
                sh  '''
                    # PACKAGE=${APP_NAME}-${VERSION}-${JENKINS_TAG}-runner.jar                                         
                    curl -v -f -u ${NEXUS_CREDS} http://${NEXUS_SERVICE_SERVICE_HOST}:${NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE} -o ${PACKAGE}
                    # TODO think about labeling of images for version purposes 
                    # oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"imageLabels\\":[{\\"name\\":\\"THINGY\\",\\"value\\":\\"MY_AWESOME_THINGY\\"},{\\"name\\":\\"OTHER_THINGY\\",\\"value\\":\\"MY_OTHER_AWESOME_THINGY\\"}]}}}"

                    oc start-build ${APP_NAME} --from-archive=${PACKAGE} --follow
                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:latest ${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                '''
            }
        }

        stage("Deploy (ArgoCD)") {
            agent {
                node {
                    label "jenkins-slave-argocd"
                }
            }
            steps {
                echo '### Commit new image tag to git ###'
                sh  '''
                    git clone https://github.com/eformat/pet-battle-api.git
                    cd pet-battle-api
                    yq w -i chart/Chart.yaml 'version' ${JENKINS_TAG}
                    git config --global user.email "jenkins@rht-labs.bot.com"
                    git config --global user.name "Jenkins"
                    git add chart/Chart.yaml
                    git commit -m "🚀 AUTOMATED COMMIT - Deployment new app version ${JENKINS_TAG} 🚀"
                    git push https://${GIT_CREDENTIALS_USR}:${GIT_CREDENTIALS_PSW}@github.com/eformat/pet-battle-api.git
                '''

                echo '### Ask ArgoCD to Sync the changes and roll it out ###'
                sh '''
                    # 1. Check if app of apps exists, if not create?
                    # 1.1 Check sync not currently in progress . if so, kill it

                    # 2. sync argocd to change pushed in previous step
                    ARGOCD_INFO="--auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure"
                    argocd app sync catz ${ARGOCD_INFO}

                    # todo sync child app 
                    argocd app sync pb-front-end ${ARGOCD_INFO}
                    argocd app wait pb-front-end ${ARGOCD_INFO}
                '''
            }
        }
    }
}
