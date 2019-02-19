package mill
package contrib.jacoco

import coursier.core._
import mill.scalalib._

trait JacocoModule extends JavaModule { self: TestModule =>

  def forkArgs = T { super.forkArgs() :+ jacocoAgentArg() }

  def jacocoOutputFile = T { T.ctx().dest / "jacoco.exec" }

  def jacocoAgentArg: T[String] = T {
    "-javaagent:"+ jacocoAgentJarPath()+"=destfile="+jacocoOutputFile()+",excludes=*.module-info"
  }

  def jacocoVersion: T[String] = "0.8.3"

  def jacocoAgentJarPath: T[String] = T {
    jacocoAgentJar()
      .map(_.path.toIO.getAbsolutePath)
      .mkString(java.io.File.pathSeparator)
  }

  def jacocoAgentJar: T[Agg[PathRef]] = T {
    Lib.resolveDependencies(
      repositories,
      Lib.depToDependencyJava(_),
      jacocoAgentDep()
    )
  }

  def jacocoAgentDep = T {
    Agg {
      ivy"org.jacoco:org.jacoco.agent:${jacocoVersion()}".configure(Attributes("jar", "runtime"))
    }
  }

  def jacocoCliJar: T[Agg[PathRef]] = T {
    Lib.resolveDependencies(
      repositories,
      Lib.depToDependencyJava(_).copy(transitive = false),
      jacocoCliDep()
    )
  }

  def jacocoCliDep = T {
    Agg {
      ivy"org.jacoco:org.jacoco.cli:${jacocoVersion()}".configure(Attributes("jar", "nodeps"))
    }
  }

  def jacocoHtmlDirectory = T { T.ctx().dest }

  def jacocoXmlDirectory = T { T.ctx().dest }

  def jacocoExecInfo = T {
    os.proc(
      'java,
      "-jar", jacocoCliJar().map(_.path.toIO.getAbsolutePath).mkString(java.io.File.pathSeparator),
      "execinfo",
      jacocoOutputFile().toIO.getAbsolutePath.toString
    ).call(millSourcePath)
  }

  def jacocoReport = T {
    os.proc(
      'java,
      "-jar", jacocoCliJar().map(_.path.toIO.getAbsolutePath).mkString(java.io.File.pathSeparator),
      "report",
      jacocoOutputFile().toIO.getAbsolutePath.toString,
      upstreamAssemblyClasspath().filter(p => os.exists(p.path)).toList.flatMap(p => List("--classfiles", p.path.toIO.getAbsolutePath)),
      "--html", jacocoHtmlDirectory().toIO.getAbsolutePath.toString,
      "--xml", jacocoXmlDirectory().toIO.getAbsolutePath.toString,
    ).call(millSourcePath)
  }
}
