package scalan.compilation.lms.scalac

import java.io.File

import scalan.ScalanCommunityDslExp
import scalan.compilation.GraphVizConfig
import scalan.compilation.lms.CommunityBridgeScala
import scalan.util.{ExtensionFilter, FileUtil}

trait CommunityLmsCompilerScala extends LmsCompilerScala with CommunityBridgeScala { self: ScalanCommunityDslExp =>

  override protected def doBuildExecutable[A, B](sourcesDir: File, executableDir: File, functionName: String, graph: PGraph, graphVizConfig: GraphVizConfig)
                                                (compilerConfig: CompilerConfig, eInput: Elem[A], eOutput: Elem[B]) = {

    val libsDir = FileUtil.file(FileUtil.currentWorkingDir, libs)
    val executableLibsDir = FileUtil.file(executableDir, libs)
    // unused
    var mainJars = methodReplaceConf.libPaths.map {
      j => FileUtil.file(libsDir, j).getAbsolutePath
    }
    var extensionsJars = Set.empty[String]
    val dir = FileUtil.listFiles(libsDir, ExtensionFilter("jar"))
    dir.foreach(f => {
      mainJars = mainJars + f.getAbsolutePath
      FileUtil.copyToDir(f, executableLibsDir)
    })

    super.doBuildExecutable[A, B](sourcesDir, executableDir, functionName, graph, graphVizConfig)(compilerConfig, eInput, eOutput)
  }

}
