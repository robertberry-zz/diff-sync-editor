package lib

import java.util
import scala.collection.JavaConversions

/** For some reason the Google algorithm uses this data structure, which has a horrid API */
object LinkedListHelper {
  def toSeq[A](as: util.LinkedList[A]) = JavaConversions.asScalaIterator(as.listIterator()).toSeq

  def fromTraversable[A](iterable: Traversable[A]) = {
    val list = new util.LinkedList[A]()
    iterable.foreach(a => list.addLast(a))
    list
  }
}
