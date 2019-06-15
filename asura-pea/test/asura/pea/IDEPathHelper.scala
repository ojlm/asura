package asura.pea

import java.nio.file.{Path, Paths}

import io.gatling.commons.util.PathHelper._

object IDEPathHelper {

  val projectRootDir: Path = Paths.get(".")
  val binariesFolder = projectRootDir / "asura-pea" / "target" / "scala-2.12" / "test-classes"
  val resultsFolder = projectRootDir / "asura-pea" / "target" / "results"
}
