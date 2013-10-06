import org.specs2.mutable._

import com.mariussoutier.storm.topology._
import com.mariussoutier.storm.topology.Implicits._

import backtype.storm._
import backtype.storm.testing._
import backtype.storm.tuple._
import backtype.storm.utils.Utils

class Test extends Specification {

  val dslDemo = StormTopologyDsl.buildTopology { implicit topology =>

    // Declare spouts
    val spout1 = addSpout (new TestWordSpout(true), "1") x 2
    val spout2 = addSpout (new TestWordSpout(true), "2") x 2

    //Declare
    val wordCountBolt1 = addBolt (new TestWordCounter, "3") x 2
    // Shuffle default stream from spout to bolt
    spout1 >~> wordCountBolt1

    // Fields grouping + declare inline
    val wordCountBolt2 = spout2 >=> ((addBolt (new TestWordCounter, "4") x 2), Seq("words"), "my-stream")

    // Global grouping; default component id = class name
    val globalCount = addBolt(new TestGlobalCount) x 1
    wordCountBolt1 |> globalCount
    wordCountBolt2 |> globalCount
  }


  class WordCountAsInStormTopologyJavaDoc extends StormTopologyDsl {

    val spout1 = addSpout (new TestWordSpout(true), "1") x 5
    val spout2 = addSpout (new TestWordSpout(true), "2") x 3

    val wordCountBolt = addBolt (new TestWordCounter, "3") x 3
    spout1 >=> (wordCountBolt, Seq("word"))
    spout2 >=> (wordCountBolt, Seq("word"))

    val globalCount = addBolt(new TestGlobalCount, "4") x 1
    spout1 |> globalCount

  }

  val wordCountFromJavaDoc = new WordCountAsInStormTopologyJavaDoc().build()

  val conf = new java.util.HashMap[String, Any]
  conf.put(Config.TOPOLOGY_WORKERS, 4)
  conf.put(Config.TOPOLOGY_DEBUG, true)
  val cluster = new LocalCluster
  cluster.submitTopology("mytopology", conf, wordCountFromJavaDoc)
  Utils.sleep(3000)
  cluster.shutdown()

  "Counting words with xumingming's test case" should {

    val wordCount = StormTopologyDsl.buildTopology { implicit topology =>

      val spout = addSpout (new TestWordSpout(true), "1") x 3

      val wordCountBolt = addBolt (new TestWordCounter, "2") x 4
      spout >=> (wordCountBolt, Seq("word"))

      spout >~> (addBolt (new TestGlobalCount, "3") x 1)

      wordCountBolt |> (addBolt(new TestAggregatesCounter, "4") x 1)

    }

    "work" in {

      val mkClusterParam = new MkClusterParam()
      mkClusterParam.setSupervisors(2)
      val daemonConf = new Config
      daemonConf.put(Config.SUPERVISOR_ENABLE, java.lang.Boolean.FALSE)
      mkClusterParam.setDaemonConf(daemonConf)

      Testing.withLocalCluster(new TestJob() {
          override def run(cluster: ILocalCluster): Unit = {

            val mockedSources = new MockedSources
            mockedSources.addMockData("1",
              new Values("nathan"),
              new Values("bob"),
              new Values("joey"),
              new Values("nathan")
            )

            val conf = new Config
            conf.setNumWorkers(2)
            conf.setDebug(true)

            val completeTopologyParam = new CompleteTopologyParam
            completeTopologyParam.setTopologyName("test")
            completeTopologyParam.setMockedSources(mockedSources)
            completeTopologyParam.setStormConf(conf)

            val result = Testing.completeTopology(cluster, wordCount, completeTopologyParam)

            (Testing.multiseteq(new Values(new Values("nathan"),
            new Values("bob"), new Values("joey"), new Values(
                "nathan")), Testing.readTuples(result, "1"))) should beTrue

            (Testing.multiseteq(new Values(new Values("nathan", 1:Integer),
                new Values("nathan", 2: Integer), new Values("bob", 1: Integer),
                new Values("joey", 1: Integer)), Testing.readTuples(result, "2")))  should beTrue

            (Testing.multiseteq(new Values(new Values(1: Integer), new Values(2: Integer),
                new Values(3: Integer), new Values(4: Integer)), Testing.readTuples(
                result, "3")))  should beTrue

            (Testing.multiseteq(new Values(new Values(1: Integer), new Values(2: Integer),
                new Values(3: Integer), new Values(4: Integer)), Testing.readTuples(
                result, "4"))) should beTrue
          }
        })

      ok
    }
  }

}
