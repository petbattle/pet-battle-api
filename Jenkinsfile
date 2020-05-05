pipeline {

    agent {
        // label "" also could have been 'agent any' - that has the same meaning.
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        HELM_REPO="http://nexus.nexus.svc.cluster.local:8081/repository/helm-charts/"
        JENKINS_TAG = "${JOB_NAME}.${BUILD_NUMBER}".replace("%2F", "-")
        JOB_NAME = "${JOB_NAME}".replace("/", "-")
        GIT_SSL_NO_VERIFY = true
        GIT_URL = "https://github.com/eformat/pet-battle-api.git"
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
                    env.APP_NAME = "pet-battle-api"
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
                    env.APP_NAME = "pet-battle-api-dev"
                }
            }
        }
        stage("prepare environment for test deploy") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH ==~ /test\/jenkins/ }
            }
            steps {
                script {
                    // Arbitrary Groovy Script executions can do in script tags
                    env.PROJECT_NAMESPACE = "labs-dev"
                    env.APP_NAME = "pet-battle-api-test"
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
                expression {
                    def retVal = sh(returnStatus: true, script: "oc -n \"${PIPELINES_NAMESPACE}\" get applications.argoproj.io \"${APP_NAME}\" -o name")
                    if (retVal == null || retVal == "") {
                        return 0;
                    }
                    return 1;
                }
            }
            steps {
                echo '### Create ArgoCD App ###'
                sh '''
                    cat <<EOF | oc apply -n ${PIPELINES_NAMESPACE} -f -
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  finalizers:
  - resources-finalizer.argocd.argoproj.io
  name: ${APP_NAME}
spec:
  destination:
    namespace: ${PROJECT_NAMESPACE}
    server: https://kubernetes.default.svc
  project: default
  source:
    helm:
      releaseName: ${APP_NAME}
    path: ''
    repoURL: ${HELM_REPO}
    targetRevision: ${JENKINS_TAG}
    chart: ${APP_NAME}
    automated:
      prune: true
      selfHeal: true
      validate: true
  ignoreDifferences:
  - group: apps.openshift.io
    jsonPointers:
    - /spec/template/spec/containers/0/image
    - /spec/triggers/0/imageChangeParams/lastTriggeredImage
    - /spec/triggers/1/imageChangeParams/lastTriggeredImage
    kind: DeploymentConfig
EOF
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
                checkout([$class: 'GitSCM',
                          branches: [[name: "${GIT_BRANCH}"]],
                          userRemoteConfigs: [[url: "${GIT_URL}", credentialsId:"${GIT_CREDENTIALS}"]]
                ]);

                echo '### configure ###'
                script {
                    // repoint nexus
                    settings = readFile("/home/jenkins/.m2/settings.xml")
                    def newsettings = settings.replace("<url>http://nexus:8081/repository/maven-public/</url>","<url>http://nexus-service:8081/repository/maven-public/</url>")
                    writeFile file: "/tmp/settings.xml", text: "${newsettings}"
                    // versions
                    def VERSION = sh script: 'mvn help:evaluate -Dexpression=project.version -s /tmp/settings.xml -q -DforceStdout', returnStdout: true
                    env.PACKAGE = "${APP_NAME}-${VERSION}-${JENKINS_TAG}.tar.gz"
                    // we want jdk.11 - for now in :4.3 slave-mvn
                    env.JAVA_HOME="/usr/lib/jvm/java-11-openjdk"
                }
                sh 'printenv'

                echo '### Running tests ###'
                // sh 'mvn test'

                echo '### Running build ###'
                sh '''                    
                    mvn package -DskipTests -s /tmp/settings.xml
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

        stage("Create OpenShift Build") {
            agent {
                node {
                    label "jenkins-slave-argocd"
                }
            }
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject("${PIPELINES_NAMESPACE}") {
                            return !openshift.selector("bc", "${APP_NAME}").exists();
                        }
                    }
                }
            }
            steps {
                echo '### Create BuildConfig ###'
                sh  '''
                    # oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"imageLabels\\":[{\\"name\\":\\"THINGY\\",\\"value\\":\\"MY_AWESOME_THINGY\\"},{\\"name\\":\\"OTHER_THINGY\\",\\"value\\":\\"MY_OTHER_AWESOME_THINGY\\"}]}}}"
                    oc new-build --binary --name=${APP_NAME} -l app=${APP_NAME} --strategy=docker --dry-run -o yaml > /tmp/bc.yaml
                    yq w -i /tmp/bc.yaml items[1].spec.strategy.dockerStrategy.dockerfilePath Dockerfile.jvm
                    oc apply -f /tmp/bc.yaml
                '''
            }
        }

        stage("Bake (OpenShift Build)") {
            agent {
                node {
                    label "jenkins-slave-argocd"
                }
            }
            steps {
                echo '### Get Binary from Nexus and shove it in a box ###'
                sh  '''
                    # PACKAGE=${APP_NAME}-${VERSION}-${JENKINS_TAG}-runner.jar                                         
                    curl -v -f -u ${NEXUS_CREDS} http://${NEXUS_SERVICE_SERVICE_HOST}:${NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE} -o ${PACKAGE}
                    oc start-build ${APP_NAME} --from-archive=${PACKAGE} --follow
                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:latest ${PROJECT_NAMESPACE}/${APP_NAME}:${JENKINS_TAG}
                '''
            }
        }

        stage("Upload Helm Chart") {
            agent {
                node {
                    label "jenkins-slave-helm"
                }
            }
            steps {
                echo '### Commit new image tag to git ###'
                sh  '''
                    git clone ${GIT_URL} && cd pet-battle-api
                    git checkout ${GIT_BRANCH}
                    
                    yq w -i chart/Chart.yaml 'appVersion' ${JENKINS_TAG}
                    yq w -i chart/values.yaml 'image_repository' 'image-registry.openshift-image-registry.svc:5000'
                    yq w -i chart/values.yaml 'image_name' ${APP_NAME}
                    yq w -i chart/values.yaml 'image_namespace' ${PROJECT_NAMESPACE}
                    
                    git config --global user.email "jenkins@rht-labs.bot.com"
                    git config --global user.name "Jenkins"
                    git add chart/Chart.yaml chart/values.yaml
                    git commit -m "🚀 AUTOMATED COMMIT - Deployment new app version ${JENKINS_TAG} 🚀"
                    git push --set-upstream origin test/jenkins https://${GIT_CREDENTIALS_USR}:${GIT_CREDENTIALS_PSW}@github.com/eformat/pet-battle-api.git
                '''

                echo '### Upload Chart ###'
                sh  '''
                    helm package chart/                    
                    curl -vvv -u ${NEXUS_CREDS} ${HELM_REPO} --upload-file ${APP_NAME}-${JENKINS_TAG}.tgz                    
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
                echo '### Ask ArgoCD to Sync the changes and roll it out ###'
                sh '''
                    # 1 Check sync not currently in progress . if so, kill it
                    # TODO
                    # 2. sync argocd to change pushed in previous step
                    ARGOCD_INFO="--auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure"                                         
                    argocd app sync ${APP_NAME} ${ARGOCD_INFO}
                    argocd app wait ${APP_NAME} ${ARGOCD_INFO}
                '''
            }
        }
    }
}
