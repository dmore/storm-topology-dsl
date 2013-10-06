package com.mariussoutier.storm.topology

import backtype.storm._
import backtype.storm.topology._
import backtype.storm.generated.StormTopology
import backtype.storm.tuple.Fields
import backtype.storm.utils.Utils

/**
 * Defines the grouping methods available on both spouts and bolts. They are meant to be used from
 * left-to-right, i.e. `spout/bolt -> tuple grouping -> bolt`.
 * There are also symbolic representations of the named tuples because they can be easier to recognize
 * when looking at the whole topology. YMMV. The following symbols are available:
 * {{{
 * >~> shuffleGrouping
 * >-> directGrouping
 * >=> fieldsGrouping
 * |> globalGrouping
 * >> noneGrouping
 * }}}
*/
trait BoltGroupings {

  val sourceName: String

  def shuffleGrouping(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    BoltGrouper(bolt.boltName, bolt.declarer.shuffleGrouping(sourceName, streamId))

  def >~>(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    shuffleGrouping(bolt, streamId)

  def globalGrouping(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    BoltGrouper(bolt.boltName, bolt.declarer.globalGrouping(sourceName, streamId))

  def |>(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    globalGrouping(bolt, streamId)

  def fieldsGrouping(bolt: BoltGrouper, fields: Seq[String], streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper = {
    val wrappedFields = new Fields(fields:_*)
    BoltGrouper(bolt.boltName, bolt.declarer.fieldsGrouping(sourceName, streamId, wrappedFields))
  }

  def >=>(bolt: BoltGrouper, fields: Seq[String], streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    fieldsGrouping(bolt, fields, streamId)

  def directGrouping(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    BoltGrouper(bolt.boltName, bolt.declarer.directGrouping(sourceName, streamId))

  def >->(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    directGrouping(bolt, streamId)

  def noneGrouping(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    BoltGrouper(bolt.boltName, bolt.declarer.noneGrouping(sourceName, streamId))

  def >>(bolt: BoltGrouper, streamId: String = Utils.DEFAULT_STREAM_ID): BoltGrouper =
    noneGrouping(bolt, streamId)

}

/**
 * Prepares spout details. Builds a [[backtype.storm.SpoutDeclarer]] after invoking the `x` method
 * {{{
 * val mySpout: DeclaredSpout = topology.addSpout(...) x 3
 * }}}
 */
class SpoutBuilder(spoutName: String, spout: IRichSpout)(implicit topology: TopologyBuilder) {
  /** Declares the spout with the given parallelism hint */
  def x(parallelism: Int) = {
    val declarer = topology.setSpout(spoutName, spout, parallelism)
    DeclaredSpout(spoutName, declarer)
  }
}

/** Contains the declarer and can be used to define groupings on bolts */
case class DeclaredSpout(spoutName: String, declarer: SpoutDeclarer) extends BoltGroupings {
  val sourceName = spoutName
}

/**
 * Prepares bolt declaration. Builds a [[backtype.storm.BoltDeclarer]] after invoking the `x` method
 * {{{
 * val myBolt: BoltGrouper = topology.addBolt(...) x 1
 * }}}
 */
case class BoltBuilder(boltName: String, bolt: IRichBolt)(implicit topology: TopologyBuilder) {
  /** Declares the bolt with the given parallelism hint */
  def x(parallelism: Int) = {
    val declarer = topology.setBolt(boltName, bolt, parallelism)
    BoltGrouper(boltName, declarer)
  }
}

/** Contains the declarer and can be used to define groupings on bolts */
case class BoltGrouper(boltName: String, declarer: BoltDeclarer) extends BoltGroupings {
  val sourceName = boltName
}

class StormTopologyDsl {

  private implicit val builder = new TopologyBuilder

  def build(): StormTopology = {
    builder.createTopology()
  }

  def addSpout(spout: IRichSpout, optSpoutId: Option[String] = None) = {
    val spoutId = optSpoutId getOrElse spout.getClass.getSimpleName
    new SpoutBuilder(spoutId, spout)(builder)
  }

  def addBolt(bolt: IBasicBolt, optBoltId: Option[String]) = {
    val boltId = optBoltId getOrElse bolt.getClass.getSimpleName
    new BoltBuilder(boltId, new BasicBoltExecutor(bolt))(builder)
  }

  def addBolt(bolt: IRichBolt, optBoltId: Option[String]) = {
    val boltId = optBoltId getOrElse bolt.getClass.getSimpleName
    new BoltBuilder(boltId, bolt)(builder)
  }

}

object StormTopologyDsl {

  def buildTopology(f: StormTopologyDsl => Unit): StormTopology = {
    val dsl = new StormTopologyDsl
    f(dsl)
    dsl.build()
  }

}

