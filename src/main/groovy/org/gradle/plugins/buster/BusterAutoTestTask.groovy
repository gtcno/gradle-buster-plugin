package org.gradle.plugins.buster

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.buster.internal.BusterConfigParser
import org.gradle.plugins.buster.internal.BusterWatcher

class BusterAutoTestTask extends  DefaultTask {
    static NAME = "busterAutoTest"


    @TaskAction
    void test() {
        def busterJs = resolveBusterJs()
        def globPatterns = new BusterConfigParser().parseGlobPatterns(busterJs.text)


        Closure listener = {kind, path ->
            project.logger.info("Starting testrun due to change in path: $path")
            def busterConfig = project.convention.getPlugin(BusterPluginConvention).busterConfig

            def execResult = project.exec {
                executable "buster"
                args = ["test", "--reporter", "specification", "--server", "http://localhost:${busterConfig.port}", "--config", busterJs]
                ignoreExitValue = true
            }
        }

        BusterWatcher.create(project, project.projectDir.absolutePath, globPatterns, listener).processEvents()
    }


    private File resolveBusterJs() {
        def busterConfig = project.convention.getPlugin(BusterPluginConvention).busterConfig
        File defaultBuster =
            ["buster.js", "test/buster.js", "spec/buster.js"].collect{new File(project.projectDir, it)}.find{it.exists()}
        File busterJsFile = busterConfig.configFile?: defaultBuster

        if(!busterJsFile) {
            throw new IllegalArgumentException("No buster config file found and no config file specified in options")
        }

        if(!busterJsFile.exists()) {
            throw new IllegalArgumentException("Config file for buster ${busterJsFile.absolutePath} could not be found")
        }

        busterJsFile
    }


}
