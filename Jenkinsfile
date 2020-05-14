pipeline {

    agent {
        label "master"
    }

    environment {
        PIPELINES_NAMESPACE = "labs-ci-cd"
        NAME = "pet-battle-api"
        IMAGE_REPOSITORY= 'image-registry.openshift-image-registry.svc:5000'
        HELM_REPO = "http://nexus.nexus.svc.cluster.local:8081/repository/helm-charts/"
        JOB_NAME = "${JOB_NAME}".replace("/", "-")
        GIT_SSL_NO_VERIFY = true
        GIT_CREDENTIALS = credentials("${PIPELINES_NAMESPACE}-git-auth")
        NEXUS_CREDS = credentials("${PIPELINES_NAMESPACE}-nexus-password")
        ARGOCD_CREDS = credentials("${PIPELINES_NAMESPACE}-argocd-token")
        NEXUS_REPO_NAME = "labs-static"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
        timeout(time: 15, unit: 'MINUTES')
        ansiColor('xterm')
        timestamps()
    }

    stages {
        stage('Perpare Environment') {
            failFast true
            parallel {
                stage("Release Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("master") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-test"
                            env.APP_NAME = "${NAME}"
                        }
                        sh 'printenv'
                    }
                }
                stage("Sandbox Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("dev") || GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("fix") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            // in multibranch the job name is just the git branch name
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                        sh 'printenv'
                    }
                }
                stage("Pull Request Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("PR-") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                        sh 'printenv'
                    }
                }
            }
        }

        stage("Build (Compile App)") {
            agent {
                node {
                    label "jenkins-slave-mvn"
                }
            }
            steps {
                echo '### configure ###'
                script {
                    // repoint nexus
                    settings = readFile("/home/jenkins/.m2/settings.xml")
                    def newsettings = settings.replace("<id>maven-public</id>", "<id>nexus</id>");
                    newsettings = newsettings.replace("<url>http://nexus:8081/repository/maven-public/</url>", "<url>http://nexus-service:8081/repository/maven-public/</url>")
                    writeFile file: "/tmp/settings.xml", text: "${newsettings}"
                    // we want jdk.11 - for now in :4.3 slave-mvn
                    env.JAVA_HOME = "/usr/lib/jvm/java-11-openjdk"
                    env.VERSION = sh(returnStdout: true, script: "mvn -s /tmp/settings.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | egrep -v INFO").trim()
                    env.PACKAGE = "${APP_NAME}-${VERSION}.tar.gz"
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
                sh '''
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
                sh '''
                    curl -v -f -u ${NEXUS_CREDS} http://${NEXUS_SERVICE_SERVICE_HOST}:${NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE} -o ${PACKAGE}
                    oc start-build ${APP_NAME} --from-archive=${PACKAGE} --follow
                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:latest ${TARGET_NAMESPACE}/${APP_NAME}:${VERSION}
                '''
            }
        }

        stage("Git Commit Chart") {
            agent {
                node {
                    label "jenkins-slave-argocd"
                }
            }
            when {
                expression { GIT_BRANCH.startsWith("master") }
            }
            steps {
                echo '### Commit new image tag to git ###'
                script {
                    env.SEM_VER = sh(returnStdout: true, script: "./update_version.sh chart/Chart.yaml patch").trim()
                }
                sh 'printenv'
                sh '''
                    yq w -i chart/Chart.yaml 'appVersion' ${VERSION}
                    yq w -i chart/values.yaml 'image_repository' 'image-registry.openshift-image-registry.svc:5000'
                    yq w -i chart/values.yaml 'image_name' ${APP_NAME}
                    yq w -i chart/values.yaml 'image_namespace' ${TARGET_NAMESPACE}

                    git checkout -b ${GIT_BRANCH}
                    git config --global user.email "jenkins@rht-labs.bot.com"
                    git config --global user.name "Jenkins"
                    git config --global push.default simple
                    git add chart/Chart.yaml chart/values.yaml
                    git commit -m "🚀 AUTOMATED COMMIT - Deployment new app version ${VERSION} 🚀"
                    git remote set-url origin https://${GIT_CREDENTIALS_USR}:${GIT_CREDENTIALS_PSW}@github.com/eformat/pet-battle-api.git
                    git push origin ${GIT_BRANCH}
                '''
            }
        }

        stage("Upload Helm Chart (master)") {
            agent {
                node {
                    label "jenkins-slave-helm"
                }
            }
            when {
                expression { GIT_BRANCH.startsWith("master") }
            }
            steps {
                echo '### Upload Helm Chart to Nexus ###'
                sh '''
                    git checkout ${GIT_BRANCH}
                    git pull
                    helm package chart/
                    curl -vvv -u ${NEXUS_CREDS} ${HELM_REPO} --upload-file ${APP_NAME}-${SEM_VER}.tgz
                '''
            }
        }

        stage("Deploy") {
            failFast true
            parallel {
                stage("helm3 publish and install (sandbox)") {
                    agent {
                        node {
                            label "jenkins-slave-helm"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("dev") || GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("fix") || GIT_BRANCH.startsWith("PR-") }
                    }
                    steps {
                        sh '''
                            helm lint chart
                        '''
                        // TODO - if SANDBOX, create release in rando ns
                        sh '''                            
                            helm upgrade --install ${APP_NAME} chart/ \
                                --namespace=${TARGET_NAMESPACE} \
                                --set image_version=${VERSION} \
                                --set image_name=${APP_NAME} \
                                --set image_repository=${IMAGE_REPOSITORY} \
                                --set image_namespace=${TARGET_NAMESPACE}
                        '''
                    }
                }
                stage("argocd sync (master)") {
                    agent {
                        node {
                            label "jenkins-slave-argocd"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("master") }
                    }
                    steps {
                        script {
                            def retVal = sh(returnStatus: true, script: "oc -n \"${PIPELINES_NAMESPACE}\" get applications.argoproj.io \"${APP_NAME}\" -o name")
                            if (retVal == null || retVal == "") {
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
    namespace: ${TARGET_NAMESPACE}
    server: https://kubernetes.default.svc
  project: default
  source:
    helm:
      releaseName: ${APP_NAME}
    path: chart
    repoURL: ${HELM_REPO}
    targetRevision: ${SEM_VER}
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
                            def patch = $/argocd app patch "${APP_NAME}" --patch $'{\"spec\":{\"source\":{\"targetRevision\":\"${SEM_VER}\"}}}' --type merge --auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure/$
                            sh patch
                        }
                        echo '### Ask ArgoCD to Sync the changes and roll it out ###'
                        sh '''
                            ARGOCD_INFO="--auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure"
                            argocd app sync ${APP_NAME} ${ARGOCD_INFO} --force --async --prune
                            argocd app wait ${APP_NAME} ${ARGOCD_INFO}
                        '''
                    }
                }
            }
        }

        stage("End to End Test") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH.startsWith("master") }
            }
            steps {
                sh  '''
                    echo "TODO - Run tests"
                '''
            }
        }

        stage("Promote app to Staging") {
            agent {
                node {
                    label "master"
                }
            }
            when {
                expression { GIT_BRANCH.startsWith("master") }
            }
            steps {
                sh  '''
                    echo "TODO - Run ArgoCD Sync 2 for staging env"
                '''
            }
        }
    }
}
