package models

/**
  *
  * Created by lzimmermann on 6/28/16.
  */
object Implicits {

  implicit class StringIterator(s: Iterator[String]) {

    // Changes the Strings generated by the iterator such that comments are
    // ignored. We distinguish two types:
    //
    //  "foobar # comment"
    //      -> results in "foobar"
    // " # comment"
    //      -> results in ""
    //
    // I'm just fooling, these are of course not two distinct types, I just want to make clear that comment-lines
    // resultpanel in empty Strings in the iterator and not in removing the string from the iterator!
    def withoutComment(c: Char): Iterator[String] = s.map(_.split(c)(0))

    // Removed all Lines that just consist of whitespace
    def noWSLines: Iterator[String] = s.withFilter(!_.trim().isEmpty)
  }
}
