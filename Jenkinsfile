pipeline {
    agent any

    tools {
        maven 'Maven 3.9.9'   // Adapter selon ton Jenkins
        jdk 'JDK 21'
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📦 Récupération du code source...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '🔨 Compilation du projet...'
                sh 'mvn clean compile'
            }
        }

        stage('Parallel Tests') {
            parallel {

                stage('Unit Tests') {
                    agent any
                    steps {
                        echo '🧪 Tests Unitaires...'
                        sh 'mvn test -Punit-tests'
                    }
                }

                stage('Repository Tests') {
                    agent any
                    steps {
                        echo '📂 Tests Repositories...'
                        sh 'mvn test -Prepository-tests'
                    }
                }

                stage('Controller Tests') {
                    agent any
                    steps {
                        echo '🌐 Tests Controllers...'
                        sh 'mvn test -Pcontroller-tests'
                    }
                }

                stage('Security Tests') {
                    agent any
                    steps {
                        echo '🔐 Tests Sécurité...'
                        sh 'mvn test -Psecurity-tests'
                    }
                }

            }
        }

        stage('All Tests (Global)') {
            steps {
                echo '📦 Exécution de tous les tests (global)...'
                sh 'mvn test'
            }
        }

        stage('Code Coverage') {
            steps {
                echo '📊 Génération du rapport JaCoCo...'
                sh 'mvn jacoco:report'
            }
        }

        stage('Package') {
            steps {
                echo '📦 Packaging du JAR final...'
                sh 'mvn package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo '💾 Archivage des artefacts...'
                archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            }
        }
    }

    post {

        always {
            echo '📋 Publication des rapports de tests...'

            // Rapports JUnit
            junit '**/target/surefire-reports/*.xml'

            // Couverture JaCoCo
            jacoco(
                execPattern: '**/target/jacoco.exec',
                classPattern: '**/target/classes',
                sourcePattern: '**/src/main/java',
                exclusionPattern: '**/test/**'
            )

            cleanWs()
        }

        success {
            echo '✅ Build réussi en mode PARALLÈLE !'
        }

        failure {
            echo '❌ Build échoué. Vérifiez les logs.'
        }

        unstable {
            echo '⚠️ Build instable (certains tests en erreur).'
        }
    }
}