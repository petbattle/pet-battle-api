pipeline {

    agent {
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        NAME = "pet-battle-api"

        // Argo specific
        ARGOCD_INSTANCE = "petbattle.app/uj"
        ARGOCD_APPNAME = "catz"
        ARGOCD_CONFIG_REPO = "github.com/eformat/ubiquitous-journey.git"
        ARGOCD_CONFIG_REPO_PATH = "example-deployment/values-applications.yaml"
        ARGOCD_CONFIG_REPO_BRANCH = "pipeline-test"

        // Job name contains the branch eg my-app-feature%2Fjenkins-123
        JOB_NAME = "${JOB_NAME}".replace("%2F", "-").replace("/", "-")
        IMAGE_REPOSITORY= 'image-registry.openshift-image-registry.svc:5000'

        GIT_SSL_NO_VERIFY = true

        // Credentials bound in OpenShift
        GIT_CREDS = credentials("${PIPELINES_NAMESPACE}-git-auth")
        NEXUS_CREDS = credentials("${PIPELINES_NAMESPACE}-nexus-password")
        ARGOCD_CREDS = credentials("${PIPELINES_NAMESPACE}-argocd-token")

        // Nexus Artifact repo
        NEXUS_REPO_NAME="labs-static"
        NEXUS_REPO_HELM = "helm-charts"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
        timeout(time: 15, unit: 'MINUTES')
        //ansiColor('xterm')
        //timestamps()
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
                            env.STAGING_NAMESPACE = "labs-staging"
                            env.APP_NAME = "${NAME}".replace("/", "-").toLowerCase()
                        }
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
                            // ammend the name to create 'sandbox' deploys based on current branch
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
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
                    }
                }
            }
        }

        stage("Build (Compile App)") {
            agent {
                node {
                    label "jenkins-agent-mvn-mandrel"
                }
            }
            steps {
                echo '### configure ###'
                script {
                    // repoint nexus
                    settings = readFile("/home/jenkins/.m2/settings.xml")
                    def newsettings = settings.replace("<id>maven-public</id>", "<id>nexus</id>");
                    newsettings = newsettings.replace("<url>http://nexus:8081/repository/maven-public/</url>", "<url>http://${SONATYPE_NEXUS_SERVICE_SERVICE_HOST}:${SONATYPE_NEXUS_SERVICE_SERVICE_PORT}/repository/maven-public/</url>")
                    writeFile file: "/tmp/settings.xml", text: "${newsettings}"
                    // we want jdk.11 - for now in :4.3 slave-mvn
                    env.JAVA_HOME = "/usr/lib/jvm/java-11-openjdk"
                    // version from pom.xml
                    env.VERSION = readMavenPom().getVersion()
                    env.PACKAGE = "${APP_NAME}-${VERSION}.tar.gz"
                }
                sh 'printenv'

                echo '### Running checkstyle ###'
                // sh 'mvn checkstyle:check'

                echo '### Running tests ###'
                // sh 'mvn test'

                echo '### Running build ###'
                sh '''
                    mvn package -DskipTests -s /tmp/settings.xml
                '''

                echo '### Packaging App for Nexus ###'
                sh '''                    
                    tar -zcvf ${PACKAGE} Dockerfile.jvm target/lib target/*-runner.jar
                    curl -vvv -u ${NEXUS_CREDS} --upload-file ${PACKAGE} http://${SONATYPE_NEXUS_SERVICE_SERVICE_HOST}:${SONATYPE_NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE}
                '''
            }
            // Post can be used both on individual stages and for the entire build.
        }

        stage("Bake (OpenShift Build)") {
            agent {
                node {
                    label "jenkins-agent-argocd"
                }
            }
            steps {
                echo '### Get Binary from Nexus and shove it in a box ###'
                sh '''
                    rm -rf ${PACKAGE}
                    curl -v -f -u ${NEXUS_CREDS} http://${SONATYPE_NEXUS_SERVICE_SERVICE_HOST}:${SONATYPE_NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_NAME}/${APP_NAME}/${PACKAGE} -o ${PACKAGE}

                    BUILD_ARGS=" --build-arg git_commit=${GIT_COMMIT} --build-arg git_url=${GIT_URL}  --build-arg build_url=${RUN_DISPLAY_URL} --build-arg build_tag=${BUILD_TAG}"
                    echo ${BUILD_ARGS}   
                    
                    oc get bc ${APP_NAME} || rc=$?
                    if [ $rc -eq 1 ]; then
                        echo " 🏗 no build - creating one 🏗"
                        oc new-build --binary --name=${APP_NAME} -l app=${APP_NAME} --strategy=docker --dry-run -o yaml > /tmp/bc.yaml
                        yq w -i /tmp/bc.yaml items[1].spec.strategy.dockerStrategy.dockerfilePath Dockerfile.jvm
                        oc apply -f /tmp/bc.yaml                        
                    fi
                                 
                    echo " 🏗 build found - starting it  🏗"    
                    oc start-build ${APP_NAME} --from-archive=${PACKAGE} --follow
                '''
            }
        }

        stage("Helm Package App") {
            agent {
                node {
                    label "jenkins-agent-helm"
                }
            }
            steps {
                echo '### Commit new image tag to git ###'
                script {
                    env.HELM_CHART_VERSION = sh(returnStdout: true, script: "helm show chart chart/ | egrep '^version' | awk '{print \$2}\\'").trim()
                }
                sh 'printenv'
                sh '''
                    helm lint chart
                '''
                sh '''
                    yq w -i chart/Chart.yaml 'name' ${APP_NAME}
                    yq w -i chart/Chart.yaml 'appVersion' ${VERSION}                     
                    
                    yq w -i chart/values.yaml 'image_repository' 'image-registry.openshift-image-registry.svc:5000'
                    yq w -i chart/values.yaml 'image_name' ${APP_NAME}
                    yq w -i chart/values.yaml 'image_namespace' ${TARGET_NAMESPACE}
                    yq w -i chart/values.yaml 'image_version' ${VERSION}
                '''
                sh '''
                    # package and release helm chart
                    helm package chart/ --app-version ${VERSION}                    
                    curl -v -f -u ${NEXUS_CREDS} http://${SONATYPE_NEXUS_SERVICE_SERVICE_HOST}:${SONATYPE_NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_HELM}/ --upload-file ${APP_NAME}-${HELM_CHART_VERSION}.tgz
                '''
            }
        }

        stage("Deploy") {
            failFast true
            parallel {
                stage("sandbox - helm3 publish and install") {
                    agent {
                        node {
                            label "jenkins-agent-helm"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("dev") || GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("fix") || GIT_BRANCH.startsWith("PR-") }
                    }
                    steps {
                        // TODO - if SANDBOX, create release in rando ns
                        sh '''                            
                            helm upgrade --install ${APP_NAME} \
                                --namespace=${TARGET_NAMESPACE} \
                                http://${SONATYPE_NEXUS_SERVICE_SERVICE_HOST}:${SONATYPE_NEXUS_SERVICE_SERVICE_PORT}/repository/${NEXUS_REPO_HELM}/${APP_NAME}-${HELM_CHART_VERSION}.tgz
                            oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:latest ${TARGET_NAMESPACE}/${APP_NAME}:${VERSION}                    
                        '''
                    }
                }
                stage("master - deploy") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("master") }
                    }
                    stages {
                        stage("test/staging argocd app create (master)") {
                            agent {
                                node {
                                    label "jenkins-agent-helm"
                                }
                            }
                            when {
                                expression {
                                    def retVal = sh(returnStatus: true, script: "oc -n \"${PIPELINES_NAMESPACE}\" get applications.argoproj.io \"${APP_NAME}\" -o name")
                                    return retVal != 0
                                }
                            }
                            steps {
                                echo '### Create ArgoCD App ###'
                                sh '''
                                    git clone https://${ARGOCD_CONFIG_REPO} config-repo
                                    cd config-repo
                                    git checkout ${ARGOCD_CONFIG_REPO_BRANCH}
                                    helm template ${ARGOCD_APPNAME} -f example-deployment/values-applications.yaml example-deployment/ | oc  -n ${PIPELINES_NAMESPACE} apply -f-
                                    oc tag ${PIPELINES_NAMESPACE}/${APP_NAME}:latest ${TARGET_NAMESPACE}/${APP_NAME}:${VERSION}
                                '''
                            }
                        }

                        stage("test env - argocd sync (master)") {
                            options {
                                skipDefaultCheckout(true)
                            }
                            agent {
                                node {
                                    label "jenkins-agent-argocd"
                                }
                            }
                            steps {
                                echo '### Commit new image tag to git ###'
                                sh '''
                                    # TODO - fix all this after chat with @eformat
                                    git clone https://${ARGOCD_CONFIG_REPO} config-repo
                                    cd config-repo
                                    git checkout ${ARGOCD_CONFIG_REPO_BRANCH}
                                    yq w -i ${ARGOCD_CONFIG_REPO_PATH} 'applications(name==test-pet-battle-api).source_ref' ${HELM_CHART_VERSION}
                                    git config --global user.email "jenkins@rht-labs.bot.com"
                                    git config --global user.name "Jenkins"
                                    git config --global push.default simple
                                    git add ${ARGOCD_CONFIG_REPO_PATH}
                                    git commit -m "🚀 AUTOMATED COMMIT - Deployment new app version ${VERSION} 🚀" || rc=$?
                                    git remote set-url origin  https://${GIT_CREDS_USR}:${GIT_CREDS_PSW}@${ARGOCD_CONFIG_REPO}
                                    git push -u origin ${ARGOCD_CONFIG_REPO_BRANCH}
                                '''

                                echo '### Ask ArgoCD to Sync the changes and roll it out ###'
                                sh '''                                    
                                    # 1 Check sync not currently in progress . if so, kill it
                                    # 2. sync argocd to change pushed in previous step
                                    ARGOCD_INFO="--auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure"
                                    # sync by label (fails on non built instances)
                                    # argocd app sync -l ${ARGOCD_INSTANCE}=${ARGOCD_APPNAME} ${ARGOCD_INFO}
                                    # argocd app wait -l ${ARGOCD_INSTANCE}=${ARGOCD_APPNAME} ${ARGOCD_INFO}
                                    # sync individual app
                                    argocd app sync test-${APP_NAME} ${ARGOCD_INFO}
                                    argocd app wait test-${APP_NAME} ${ARGOCD_INFO}
                                '''
                            }
                        }
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
            options {
                skipDefaultCheckout(true)
            }
            agent {
                node {
                    label "jenkins-agent-argocd"
                }
            }
            when {
                expression { GIT_BRANCH.startsWith("master") }
            }
            steps {
                sh '''
                    # TODO - fix all this after chat with @eformat
                    git clone https://${ARGOCD_CONFIG_REPO} config-repo
                    cd config-repo
                    git checkout ${ARGOCD_CONFIG_REPO_BRANCH}
                    yq w -i ${ARGOCD_CONFIG_REPO_PATH} 'applications(name==pet-battle-api).source_ref' ${HELM_CHART_VERSION}
                    git config --global user.email "jenkins@rht-labs.bot.com"
                    git config --global user.name "Jenkins"
                    git config --global push.default simple
                    git add ${ARGOCD_CONFIG_REPO_PATH}
                    # grabbing the error code incase there is nothing to commit and allow jenkins proceed
                    git commit -m "🚀 AUTOMATED COMMIT - Deployment new app version ${VERSION} 🚀" || rc=$?
                    git remote set-url origin  https://${GIT_CREDS_USR}:${GIT_CREDS_PSW}@${ARGOCD_CONFIG_REPO}
                    git push -u origin ${ARGOCD_CONFIG_REPO_BRANCH}
                '''

                echo '### Ask ArgoCD to Sync the changes and roll it out ###'
                sh '''
                    oc tag ${TARGET_NAMESPACE}/${APP_NAME}:${VERSION} ${STAGING_NAMESPACE}/${APP_NAME}:${VERSION}
                    # 1 Check sync not currently in progress . if so, kill it
                    # 2. sync argocd to change pushed in previous step
                    ARGOCD_INFO="--auth-token ${ARGOCD_CREDS_PSW} --server ${ARGOCD_SERVER_SERVICE_HOST}:${ARGOCD_SERVER_SERVICE_PORT_HTTP} --insecure"
                    # sync individual app
                    argocd app sync ${APP_NAME} ${ARGOCD_INFO}
                    argocd app wait ${APP_NAME} ${ARGOCD_INFO}                    
                '''

                sh '''
                    echo "merge versions back to the original GIT repo as they should be persisted?"
                '''
            }
        }
    }
}
