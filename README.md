# StormTopologyDsl

## Motivation

Defining [Storm](https://github.com/nathanmarz/storm) topologies with the fluent API is pretty good
when you're coming from Java.
The inverse nature (define bolt, then define the incoming tuples' grouping) can be confusing at times,
especially since everything is only connected via String keys.

In Scala, we can (IMHO) do better.

## Topologies

The `StormTopologyDsl` offers two ways of defining a topology. Either extend it, or build an
ad-hoc topology.

 ```scala
 import com.mariussoutier.storm.topology._
 class MyTopology extends StormTopologyDsl {
   ...
 }
 ```

 ```scala
 import com.mariussoutier.storm.topology._
 val myTopology: StormTopology = StormTopologyDsl.buildTopology { implicit topology =>
   ...
 }
```

The advantage of the letter is that you don't have to call `build()` yourself.

## Declaring Spouts and Bolts

Spouts are declared via the `addSpout` method. The `x` method not only passes a parallelism hint, but
must also be called to effectively declare the spout.[0]

```scala
import com.mariussoutier.storm.topology._

StormTopologyDsl.buildTopology { topology =>
  val spout = topology.addSpout(new MySpout) x 3
  val bolt = topology.addBolt(new MyBolt) x 2
}
```

Or when declaring the topology parameter implicit:

```scala
import com.mariussoutier.storm.topology._

StormTopologyDsl.buildTopology { implicit topology =>
  val spout = addSpout(new MySpout) x 3
  val bolt = addBolt(new MyBolt) x 2
}
```

Both `addSpout` and `addBolt` take another parameter, `id: String`. Setting it however is only
necessary when you are declaring the same type of spout or bolt multiple times. Otherwise, the DSL
will simply set the name to the simple class name.


## Groupings

The essence of any topology is how the tuples that are streamed between bolts are grouped. This is
done by defining groupings, and sometimes streams.

When we declared spouts and bolt, in contrast to the Java API we didn't specify a String ID.
This means we don't have to care about some assigned String but we use values to compose our topology.

To pipe a spout or bolt into another bolt, we use a left-to-right syntax.

```scala
import com.mariussoutier.storm.topology._

StormTopologyDsl.buildTopology { implicit topology =>

  mySpout shuffleGrouping myBolt

  myBolt globalGrouping (myBolt, "my-stream")

}
```

There are also symbolic (sic!) versions of the same method available.

```scala
import com.mariussoutier.storm.topology._

StormTopologyDsl.buildTopology { implicit topology =>

  mySpout >~> myBolt // shuffle

  myBolt |> (myBolt, "my-stream") // global

  myOtherBolt >=> (myOtherBolt, Seq("word")) // fields

}
```

Please refer to the tests and API docs for more examples.


[0]: Making the `x`-method do two things sounds like bad design (and it is), however it's necessary because the underlying Storm topology doesn't allow setting this value after declaring a spout or bolt.
