#!/usr/bin/env groovy
pipeline {
    agent any
    environment {
        ANDROID_HOME = '/Users/jenkins/Library/Android/sdk'
        ANDROID_BUILD_TOOLS_VERSION = '30.0.3'
        projectName = """${
            sh(script: 'IFS="/" read -ra TOKENS <<< "${JOB_NAME}"; echo ${TOKENS[0]}', returnStdout: true).trim()
        }"""
        PARSED_JOB_NAME = URLDecoder.decode(env.JOB_NAME, 'UTF-8')
    }
    stages {
        stage('Unit Tests') {
            steps {
                echo "Smoke Tests are running - ${PARSED_JOB_NAME}"
                sh '''
                        #!/bin/bash
                        ./gradlew clean build
                   '''
            }
        }

        stage('Sign apk') {
            steps {
                script {
                    sh '$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/zipalign -v -p 4 mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned.apk mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned-aligned.apk'
                    if (fileExists('mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned.apk')) {
                        sh '$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/zipalign -v -p 4 mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned.apk mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk'
                    }
                    withCredentials([string(credentialsId: 'android_store_key_pass', variable: 'JKS_PASS')]) {
                        sh '$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/apksigner sign --ks ~/google_play_key.jks --ks-pass pass:$JKS_PASS --out mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-signed.apk mopub-sample/build/outputs/apk/external/release/mopub-sample-external-release-unsigned-aligned.apk'
                        if (fileExists('mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk')) {
                            sh '$ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/apksigner sign --ks ~/google_play_key.jks --ks-pass pass:$JKS_PASS --out mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-signed.apk mopub-sample/build/outputs/apk/internal/release/mopub-sample-internal-release-unsigned-aligned.apk'
                        }
                    }
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'mopub-sample/build/outputs/**/*.apk', excludes: 'mopub-sample/build/outputs/**/*unsigned.apk, mopub-sample/build/outputs/**/*aligned.apk', onlyIfSuccessful: true
                archiveArtifacts artifacts: 'mopub-sdk/build/outputs/aar/mopub-sdk-*.aar', onlyIfSuccessful: true
                archiveArtifacts artifacts: 'mopub-sdk/mopub-sdk-*/build/outputs/aar/mopub-sdk-*.aar', onlyIfSuccessful: true
            }
        }
    }
    post {
        fixed {
            slackSend color: 'GREEN', message: "<${env.BUILD_URL}|${PARSED_JOB_NAME} #${env.BUILD_NUMBER}> has succeeded."
        }
        failure {
            slackSend color: 'RED', message: "Attention @here <${env.BUILD_URL}|${PARSED_JOB_NAME} #${env.BUILD_NUMBER}> has failed."
        }
    }
}
