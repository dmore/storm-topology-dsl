package com.mariussoutier.storm.topology

package object Implicits {
  implicit def wrapInOption[T](t: T): Option[T] = Option(t)
}
