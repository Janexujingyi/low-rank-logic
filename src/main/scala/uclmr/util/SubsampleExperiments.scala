package uclmr.util

import java.io.File

import ml.wolfe.util.{Conf, OverrideConfig, ProgressBar, RunExperimentSeries}

import scala.util.Random

/**
 * @author rockt
 */
object SubsampleExperiments extends App {
  val threads = args.lift(0).getOrElse("1").toInt
  val formulaeFile = args.lift(1).getOrElse("data/formulae/filtered.txt")
  val confPath = args.lift(2).getOrElse("conf/mf.conf")
  val logFilePath = args.lift(3).getOrElse("data/out/experiments.log")
  val runLogFilePath = args.lift(4).getOrElse("data/out/run.log")
  val runLogFile = new File(runLogFilePath)
  val runLogFileDir = new File(runLogFilePath.split("/").init.mkString("/"))
  runLogFileDir.mkdirs()
  runLogFile.createNewFile()


  Conf.add(OverrideConfig(Map("logFile" -> logFilePath), confPath + ".tmp", confPath))

  val rand = new Random(0l)

  val series = Map(
    "mf.subsample" -> (0 to 20).map(_ / 40.0).toSeq,
    "mf.mode" -> Seq("mf", "low-rank-logic", "pre-inference", "post-inference", "inference-only"),
    "evalConf" -> Seq("eval-subsample.conf")
  ).mapValues(rand.shuffle(_))


  import scala.sys.process._
  val userDir = System.getProperty("user.dir")

  //check whether in the right directory
  val correctDir = if (userDir.endsWith("/wolfe")) userDir else userDir.split("/").init.mkString("/")

  //first compile project for all workers so that there will be no clashes
  Process(Seq("sbt", "compile"), new File(correctDir)).!


  val progressBar = new ProgressBar(series.values.map(_.size).product, 1)
  progressBar.start()

  RunExperimentSeries(series, threads, confPath) { conf =>
    (Process(Seq(
      "sbt",
      "project wolfe-apps",
      "vmargs -Xmx8G",
      s"run-main ml.wolfe.apps.factorization.MatrixFactorization $conf"), new File(correctDir)
    ) #>> runLogFile).!!

    progressBar(conf)
  }

  System.exit(0)
}



