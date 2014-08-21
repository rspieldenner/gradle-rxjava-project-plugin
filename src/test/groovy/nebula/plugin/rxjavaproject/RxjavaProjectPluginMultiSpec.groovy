/*
 * Copyright 2014 Netflix, Inc.
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
package nebula.plugin.rxjavaproject

import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Ignore

import java.util.jar.Attributes
import java.util.jar.JarFile

class RxjavaProjectPluginMultiSpec extends IntegrationSpec {

    Grgit originGit

    def setup() {
        useToolingApi = false

        def subBuildFile = """
            ${applyPlugin(RxjavaProjectPlugin)}
            apply plugin: 'java'
        """.stripIndent()

        // Sub A
        createSubProject('SubA', subBuildFile)
        writeHelloWorld('SubA/', 'reactivey')

        // Sub B
        createSubProject('SubB', subBuildFile)
        writeHelloWorld('SubB/', 'reactivez')

        createFile('gradle.properties') << 'version=1.0.0-SNAPSHOT'
        buildFile << """
            ${applyPlugin(RxjavaProjectPlugin)}
        """.stripIndent()

        originGit = Grgit.init(dir: projectDir)
        originGit.add(patterns: ["build.gradle", 'settings.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')
    }

    def cleanup() {
        if(originGit) {
            originGit.close()
        }
    }

    def 'stand it all up'() {
        when:
        def result = runTasksSuccessfully('build')

        then:
        fileExists('SubA/build/libs/SubA-1.0.0-SNAPSHOT.jar')
        fileExists('SubB/build/libs/SubB-1.0.0-SNAPSHOT.jar')
    }

    @Ignore
    def Map<String,String> getManifest(String jarPath) {
        def jmhJar = new JarFile(new File(projectDir, jarPath))
        Attributes attrs = jmhJar.manifest.mainAttributes
        attrs.keySet().collectEntries { Object key ->
            return [key.toString(), attrs.getValue(key)]
        }
    }

    @Ignore
    def createSubProject(String name, String buildFile) {
        settingsFile << """
            include '${name}'
        """.stripIndent()

        def sub = new File(projectDir, name)
        sub.mkdirs()

        new File(sub, 'build.gradle') << buildFile

        return sub
    }

    @Ignore
    def writeHelloWorld(String pathPrefix, String packageDotted) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/HelloWorld.java'
        def javaFile = createFile("${pathPrefix}${path}")
        javaFile << """package ${packageDotted};

            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.stripIndent()
    }

}
