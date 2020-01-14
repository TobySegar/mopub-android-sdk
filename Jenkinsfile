#!/usr/bin/env groovy
def androidAutomationProjectName = "mopub-android-automation"

pipeline {
    agent any
    environment {
        ANDROID_HOME = '/Users/jenkins/Library/Android/sdk'
        projectName = """${sh(script: 'IFS="/" read -ra TOKENS <<< "${JOB_NAME}"; echo ${TOKENS[0]}', returnStdout: true).trim()}"""
    }
    stages {
        stage('Tests') {
            steps {
                script {
                    if(projectName == androidAutomationProjectName) {
                        stage('Android automation tests') {
                            echo "Android Automation Tests are running - ${env.JOB_NAME}"
                            sh '''
                                #!/bin/bash
                                chmod +x android.sh
                                ./android.sh
                            '''
                        }
                    } else {
                        stage('Smoke tests') {
                            echo "Smoke Tests are running - ${env.JOB_NAME}"
                            sh '''
                                #!/bin/bash
                                ./gradlew clean build
                            '''
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend color: 'GREEN', message: "<${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> has succeeded."
        }
        failure {
            slackSend color: 'RED', message: "Attention @here <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> has failed."
        }
    }

}

