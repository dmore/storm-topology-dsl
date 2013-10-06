package com.mariussoutier.storm

import backtype.storm.topology._


package object topology {

  /* Ad-hoc dsl */

  def addSpout(spout: IRichSpout, optSpoutId: Option[String] = None)(implicit dsl: StormTopologyDsl) =
    dsl.addSpout(spout, optSpoutId)

  // Default values don't work here :(

  def addBolt(bolt: IBasicBolt)(implicit dsl: StormTopologyDsl) =
    dsl.addBolt(bolt, None)

  def addBolt(bolt: IBasicBolt, optBoltId: Option[String])(implicit dsl: StormTopologyDsl) =
    dsl.addBolt(bolt, optBoltId)

  def addBolt(bolt: IRichBolt, optBoltId: Option[String] = None)(implicit dsl: StormTopologyDsl) =
    dsl.addBolt(bolt, optBoltId)

  def addBolt(bolt: IRichBolt)(implicit dsl: StormTopologyDsl) =
    dsl.addBolt(bolt, None)

}
